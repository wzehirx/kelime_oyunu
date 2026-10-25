package com.kelimeoyunu.ui.game

import androidx.lifecycle.ViewModel
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.CellState
import com.kelimeoyunu.domain.GameEngine
import com.kelimeoyunu.domain.Level
import com.kelimeoyunu.domain.PlacePosition
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.domain.WorldTour
import com.kelimeoyunu.domain.WordDictionary
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Oyun durumu. Ağır yükleme (seviye dosyası, sözlük, kayıt) [dispatcher] üzerinde yapılır; eylemler
 * çağıran iş parçacığında, kilit altında çalışır. Kayıtlar tek bir sıralı tüketiciyle yazılır.
 */
class GameViewModel(
    private val levels: LevelRepository,
    private val dictionaryProvider: () -> WordDictionary,
    private val progressRepository: ProgressRepository,
    private val random: Random = Random.Default,
    private val dispatcher: CoroutineDispatcher,
    private val worldTourProvider: () -> WorldTour? = { null },
) : ViewModel() {

    constructor(
        levels: LevelRepository,
        dictionary: WordDictionary,
        progressRepository: ProgressRepository,
        random: Random = Random.Default,
        dispatcher: CoroutineDispatcher,
        worldTour: WorldTour? = null,
    ) : this(levels, { dictionary }, progressRepository, random, dispatcher, { worldTour })

    private val _state = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.onSubscription {
        // Yükleme dinleyiciden önce biterse ilk ülke girişi kaybolmasın: ilk dinleyici onu alır.
        val pending = synchronized(lock) { pendingEntry.also { pendingEntry = null } }
        if (pending != null) emit(pending)
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val saves = Channel<Progress>(Channel.CONFLATED)
    private val lock = Any()

    // --- yalnızca lock altında ---
    private var dictionary: WordDictionary? = null
    private var loaded = false
    private var levelIndex = 0
    private var level: Level? = null
    private var engine: GameEngine? = null
    private var coins = 0
    private var bonusMeter = 0
    private var order: List<Int> = emptyList()
    private var selected: List<Int> = emptyList()
    private var levelPaid = false
    private var tour: WorldTour? = null
    private var lastEnteredPlace = -1
    private var pendingEntry: GameEvent.PlaceEntered? = null

    init {
        scope.launch {
            for (p in saves) {
                try {
                    progressRepository.save(p)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Kayıt başarısızsa oyun sürer; bir sonraki kayıt yeniden dener.
                }
            }
        }
        scope.launch { load() }
    }

    private suspend fun load() {
        try {
            val (dict, saved) = withContext(dispatcher) {
                val d = dictionaryProvider()
                if (levels.count == 0) error("no levels")
                // Tur yüklenemezse (ya da hata verirse) oyun eski davranışla sürer.
                tour = try { worldTourProvider() } catch (_: Exception) { null }
                d to progressRepository.progress.first()
            }
            synchronized(lock) {
                dictionary = dict
                coins = saved.coins
                bonusMeter = saved.bonusMeter.coerceIn(0, BONUS_TARGET - 1)
                loaded = true
                if (saved.levelIndex >= levels.count) {
                    levelIndex = levels.count
                    _state.value = GameUiState.AllDone
                } else {
                    startLevel(saved.levelIndex, saved)
                    publish()
                    announceEntry(deferIfNoListener = true)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _state.value = GameUiState.Error("Oyun verisi yüklenemedi. Uygulamayı yeniden başlatın.")
        }
    }

    private fun startLevel(index: Int, saved: Progress?) {
        val lv = levels.get(index)!!
        levelIndex = index
        level = lv
        val e = GameEngine(
            lv, dictionary!!,
            saved?.found.orEmpty(), saved?.bonusFound.orEmpty(), saved?.revealed.orEmpty(), random,
        )
        engine = e
        order = lv.letters.indices.toList()
        selected = emptyList()
        // Kayıtlı seviye zaten bitmişse ödül daha önce verilmiştir.
        levelPaid = e.isComplete()
    }

    // ---------- eylemler ----------

    fun onLetter(index: Int) = act { lv, e ->
        if (e.isComplete() || index !in lv.letters.indices || index in selected) return@act
        selected = selected + index
        publish()
        emit(GameEvent.LetterAdded(lv.letters[index], index, selected.size))
    }

    fun onDelete() = act { _, _ ->
        if (selected.isEmpty()) return@act
        selected = selected.dropLast(1)
        publish()
        emit(GameEvent.Deleted)
    }

    fun onClear() = act { _, _ ->
        if (selected.isEmpty()) return@act
        selected = emptyList()
        publish()
        emit(GameEvent.Cleared)
    }

    fun onSubmit() = act { _, e ->
        if (e.isComplete()) return@act
        val result = e.submit(currentWord())
        emit(GameEvent.Submitted(result))
        when (result) {
            is SubmitResult.Correct -> {
                selected = emptyList()
                completeIfDone()
                commit()
            }
            is SubmitResult.Bonus -> {
                selected = emptyList()
                bonusMeter++
                if (bonusMeter >= BONUS_TARGET) {
                    bonusMeter = 0
                    coins += BONUS_REWARD
                    emit(GameEvent.BonusMeterFull)
                    emit(GameEvent.CoinsEarned(BONUS_REWARD))
                }
                commit()
            }
            is SubmitResult.AlreadyFound -> {
                selected = emptyList()
                publish()
            }
            is SubmitResult.Invalid, SubmitResult.Empty -> Unit
        }
    }

    fun onShuffle() = act { _, _ ->
        var next = order.shuffled(random)
        var tries = 0
        while (next == order && order.size > 1 && tries++ < 5) next = order.shuffled(random)
        order = next
        publish()
        emit(GameEvent.Shuffled)
    }

    fun onHintLetter() = act { lv, e ->
        if (e.isComplete()) return@act
        val anyHidden = lv.words.any { w -> w.cells().any { (r, c) -> e.cellState(r, c) == CellState.Hidden } }
        if (!anyHidden) return@act emit(GameEvent.NoHintAvailable)
        if (coins < HINT_LETTER_COST) return@act emit(GameEvent.NotEnoughCoins)
        e.revealLetter() ?: return@act emit(GameEvent.NoHintAvailable)
        coins -= HINT_LETTER_COST
        emit(GameEvent.HintUsed(coins))
        commit()
    }

    fun onHintWord() = act { _, e ->
        if (e.isComplete()) return@act
        if (coins < HINT_WORD_COST) return@act emit(GameEvent.NotEnoughCoins)
        e.revealWord() ?: return@act emit(GameEvent.NoHintAvailable)
        coins -= HINT_WORD_COST
        emit(GameEvent.HintUsed(coins))
        completeIfDone()
        commit()
    }

    fun onNextLevel() = act { _, e ->
        if (!e.isComplete()) return@act
        val next = levelIndex + 1
        if (next >= levels.count) {
            levelIndex = levels.count
            level = null
            engine = null
            _state.value = GameUiState.AllDone
            save()
            emit(GameEvent.AllLevelsDone)
        } else {
            startLevel(next, null)
            commit()
            announceEntry(deferIfNoListener = false)
        }
    }

    // ---------- yardımcılar ----------

    private inline fun act(block: (Level, GameEngine) -> Unit) {
        synchronized(lock) {
            val lv = level ?: return
            val e = engine ?: return
            if (!loaded) return
            block(lv, e)
        }
    }

    /** Seviye sayısı; yalnızca yükleme bittikten sonra (AllDone/Ready) çağrılmalıdır. */
    val levelCount: Int get() = levels.count

    private fun emit(event: GameEvent) {
        _events.tryEmit(event)
    }

    private fun currentWord(): String {
        val lv = level ?: return ""
        return selected.joinToString("") { lv.letters[it].toString() }
    }

    private fun completeIfDone() {
        val e = engine ?: return
        if (e.isComplete() && !levelPaid) {
            levelPaid = true
            selected = emptyList()
            coins += LEVEL_REWARD
            emit(GameEvent.CoinsEarned(LEVEL_REWARD))
            emit(GameEvent.LevelCompleted)
            completePlaceIfDone()
        }
    }

    private fun place(): PlacePosition? = tour?.positionOf(levelIndex)

    /** Ülkenin ilk seviyesi açıldıysa ve bu ülke bu ViewModel'de daha duyurulmadıysa [GameEvent.PlaceEntered] verir. */
    private fun announceEntry(deferIfNoListener: Boolean) {
        val p = place() ?: return
        if (p.levelInPlace != 1 || p.placeIndex == lastEnteredPlace) return
        lastEnteredPlace = p.placeIndex
        val event = GameEvent.PlaceEntered(p.place, p.placeLevelCount)
        if (deferIfNoListener && _events.subscriptionCount.value == 0) pendingEntry = event else emit(event)
    }

    private fun completePlaceIfDone() {
        val t = tour ?: return
        val p = place() ?: return
        if (p.levelInPlace != p.placeLevelCount) return
        coins += PLACE_REWARD
        emit(GameEvent.PlaceCompleted(p.place, PLACE_REWARD, t.place(p.placeIndex + 1)?.ad))
    }

    private fun commit() {
        publish()
        save()
    }

    private fun save() {
        val s = engine?.snapshot()
        saves.trySend(
            Progress(
                levelIndex = levelIndex,
                coins = coins,
                found = s?.found.orEmpty(),
                bonusFound = s?.bonusFound.orEmpty(),
                revealed = s?.revealed.orEmpty(),
                bonusMeter = bonusMeter,
            )
        )
    }

    private fun publish() {
        val lv = level ?: return
        val e = engine ?: return
        val found = e.snapshot().found
        val rows = lv.words.mapIndexed { i, w ->
            val cells = w.cells().mapIndexed { k, (r, c) ->
                if (e.cellState(r, c).let { it == CellState.Filled || it == CellState.Revealed }) w.word[k] else null
            }
            WordRow(i + 1, w.word.length, cells, w.word in found)
        }
        _state.value = GameUiState.Ready(
            levelNumber = levelIndex + 1,
            tiles = order.map { LetterTile(it, lv.letters[it], it in selected) },
            currentWord = currentWord(),
            coins = coins,
            remainingWords = e.remainingWords,
            bonusMeter = bonusMeter,
            bonusTarget = BONUS_TARGET,
            wordRows = rows,
            levelComplete = e.isComplete(),
            place = place(),
        )
    }

    override fun onCleared() {
        saves.close() // bekleyen son kayıt yazılır, sonra tüketici biter
    }

    companion object {
        const val BONUS_TARGET = 5
        const val BONUS_REWARD = 20
        const val LEVEL_REWARD = 10
        const val PLACE_REWARD = 25
        const val HINT_LETTER_COST = 10
        const val HINT_WORD_COST = 30
    }
}
