package com.kelimeoyunu.ui.game

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.domain.WordDictionary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bellek içi DataStore: deterministik, gerçek IO yok. Kayıt hatası enjekte edilebilir. */
private class FakeStore(
    initial: Preferences = emptyPreferences(),
    var failNextUpdates: Int = 0,
    var updates: Int = 0,
) : DataStore<Preferences> {
    val flow = MutableStateFlow(initial)
    override val data: Flow<Preferences> = flow
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        updates++
        if (failNextUpdates > 0) { failNextUpdates--; throw IOException("disk dolu") }
        val n = transform(flow.value)
        flow.value = n
        return n
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelAdversarialTest {

    private fun gz(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }

    // Seviye 3: havuzda iki 'a' var (tekrarlı harf).
    private val threeLevels =
        "1|ten|ten,0,0,y;net,0,2,d\n2|etm|met,0,0,y;tem,0,2,d\n3|ala|ala,0,0,y;la,0,1,d\n"
    private val dupOnly = "1|ala|ala,0,0,y;la,0,1,d\n"
    private fun levels(text: String = threeLevels) = LevelRepository { ByteArrayInputStream(gz(text)) }
    private val dict = WordDictionary(setOf("ten", "net", "met", "tem", "et", "en", "te", "ne", "ent", "ala", "la", "al"))

    private class H(val vm: GameViewModel, val store: FakeStore, val events: MutableList<GameEvent>)

    private fun TestScope.start(
        store: FakeStore = FakeStore(),
        lv: LevelRepository = levels(),
        seed: Int = 1,
    ): H {
        val d = StandardTestDispatcher(testScheduler)
        val vm = GameViewModel(lv, { dict }, ProgressRepository(store), Random(seed), d)
        val ev = ArrayList<GameEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { ev += it } }
        runCurrent()
        return H(vm, store, ev)
    }

    private fun GameViewModel.ready() = state.value as GameUiState.Ready

    private fun GameViewModel.tap(vararg idx: Int) = idx.forEach { onLetter(it) }

    private fun clear(vm: GameViewModel) {
        val m = GameViewModel::class.java.getDeclaredMethod("onCleared")
        m.isAccessible = true
        m.invoke(vm)
    }

    private fun prefs(p: Progress): Preferences {
        // ProgressRepository.save ile aynı biçimi üretmek için gerçek depoya yazıp okumak gerekir;
        // burada anahtar adlarını doğrudan kullanıyoruz.
        val m = mutablePreferencesOf()
        m[androidx.datastore.preferences.core.intPreferencesKey("level_index")] = p.levelIndex
        m[androidx.datastore.preferences.core.intPreferencesKey("coins")] = p.coins
        m[androidx.datastore.preferences.core.intPreferencesKey("bonus_meter")] = p.bonusMeter
        m[androidx.datastore.preferences.core.stringPreferencesKey("found")] = p.found.joinToString(",")
        m[androidx.datastore.preferences.core.stringPreferencesKey("bonus_found")] = p.bonusFound.joinToString(",")
        m[androidx.datastore.preferences.core.stringPreferencesKey("revealed")] =
            p.revealed.joinToString(";") { (r, c) -> "$r,$c" }
        return m
    }

    // ---------- tile index / tekrarlı harf ----------

    @Test
    fun duplicateLetterTilesAreMarkedByIndexAndDeleteFreesLastAdded() = runTest {
        val h = start(lv = levels(dupOnly))
        val v = h.vm
        assertEquals(listOf('a', 'l', 'a'), v.ready().tiles.map { it.char })
        v.tap(2)
        assertEquals(listOf(false, false, true), v.ready().tiles.map { it.used })
        v.tap(0)
        assertEquals(listOf(true, false, true), v.ready().tiles.map { it.used })
        v.tap(0, 2) // ikisi de zaten seçili
        assertEquals("aa", v.ready().currentWord)
        v.onDelete() // son eklenen: index 0
        assertEquals(listOf(false, false, true), v.ready().tiles.map { it.used })
        assertEquals("a", v.ready().currentWord)
        v.tap(0) // yeniden eklenebilir
        v.tap(1)
        assertEquals("aal", v.ready().currentWord)
        v.onDelete(); v.onDelete(); v.onDelete(); v.onDelete()
        assertEquals("", v.ready().currentWord)
        assertTrue(v.ready().tiles.none { it.used })
    }

    @Test
    fun duplicateLetterWordSubmitsAndShuffleKeepsPerIndexSelection() = runTest {
        val h = start(lv = levels(dupOnly))
        val v = h.vm
        v.tap(2, 1)
        repeat(20) {
            v.onShuffle()
            assertEquals("al", v.ready().currentWord)
            assertEquals(setOf(1, 2), v.ready().tiles.filter { it.used }.map { it.index }.toSet())
            assertEquals(listOf('a', 'a', 'l'), v.ready().tiles.map { it.char }.sorted())
        }
        v.tap(0)
        assertEquals("ala", v.ready().currentWord)
        v.onSubmit()
        assertEquals(GameEvent.Submitted(SubmitResult.Correct("ala")), h.events.last { it is GameEvent.Submitted })
        assertTrue(v.ready().tiles.none { it.used })
    }

    @Test
    fun threeTapsOfSingleAGiveOnlyOneLetterWhenPoolHasOneA() = runTest {
        val h = start() // seviye 1: ten
        h.vm.tap(0, 0, 0)
        assertEquals("t", h.vm.ready().currentWord)
        assertEquals(1, h.events.count { it is GameEvent.LetterAdded })
    }

    // ---------- seviye bitişinden sonra ----------

    /** BUG adayı: seviye ipucuyla bitince seçili harfler kalır; sonra onSubmit bonus sayar ve jeton verir. */
    @Test
    fun submitAfterLevelCompleteByHintIsIgnored() = runTest {
        val store = FakeStore(prefs(Progress(0, 100, emptySet(), emptySet(), emptySet(), 4)))
        val h = start(store)
        val v = h.vm
        v.tap(1, 0) // "et": bonus kelime, henüz gönderilmedi
        v.onHintWord(); v.onHintWord() // seviyeyi bitirir
        assertTrue(v.ready().levelComplete)
        val coins = v.ready().coins
        val n = h.events.size
        v.onSubmit()
        assertEquals("seviye bittikten sonra jeton değişmemeli", coins, v.ready().coins)
        assertEquals("bonus sayacı değişmemeli", 4, v.ready().bonusMeter)
        assertEquals("olay üretilmemeli: ${h.events.drop(n)}", n, h.events.size)
    }

    @Test
    fun allActionsIgnoredAfterCompleteWhenNoSelection() = runTest {
        val h = start()
        val v = h.vm
        v.tap(0, 1, 2); v.onSubmit(); v.tap(2, 1, 0); v.onSubmit()
        assertTrue(v.ready().levelComplete)
        val s = v.state.value
        val n = h.events.size
        v.onLetter(0); v.onHintLetter(); v.onHintWord(); v.onSubmit()
        assertEquals(s, v.state.value)
        // onSubmit boşken Empty olayı üretebilir; diğerleri sessiz olmalı
        assertTrue(h.events.drop(n).all { it == GameEvent.Submitted(SubmitResult.Empty) })
        assertEquals(60, v.ready().coins)
    }

    @Test
    fun doubleNextLevelDoesNotSkipLevel() = runTest {
        val h = start()
        val v = h.vm
        v.tap(0, 1, 2); v.onSubmit(); v.tap(2, 1, 0); v.onSubmit()
        v.onNextLevel(); v.onNextLevel(); v.onNextLevel()
        assertEquals(2, v.ready().levelNumber)
        assertEquals(60, v.ready().coins)
    }

    @Test
    fun hintWordCompletingLevelPaysOnceAcrossRestart() = runTest {
        val store = FakeStore(prefs(Progress(0, 100, emptySet(), emptySet(), emptySet(), 0)))
        val h = start(store)
        h.vm.onHintWord(); h.vm.onHintWord()
        runCurrent()
        assertEquals(50, h.vm.ready().coins) // 100 - 60 + 10
        clear(h.vm); runCurrent()
        val h2 = start(store)
        assertTrue(h2.vm.ready().levelComplete)
        assertEquals(50, h2.vm.ready().coins)
        h2.vm.onHintWord(); h2.vm.onHintLetter()
        assertEquals(50, h2.vm.ready().coins)
        h2.vm.onNextLevel()
        assertEquals(2, h2.vm.ready().levelNumber)
        assertEquals(50, h2.vm.ready().coins)
    }

    // ---------- ipucu maliyeti / sınırlar ----------

    @Test
    fun exactCoinBoundaries() = runTest {
        val store = FakeStore(prefs(Progress(0, 30, emptySet(), emptySet(), emptySet(), 0)))
        val h = start(store)
        val v = h.vm
        v.onHintWord() // tam 30 -> 0
        assertEquals(0, v.ready().coins)
        v.onHintLetter(); v.onHintWord()
        assertEquals(0, v.ready().coins)
        assertEquals(listOf(GameEvent.HintUsed(0), GameEvent.NotEnoughCoins, GameEvent.NotEnoughCoins), h.events)
    }

    @Test
    fun hintLetterAtExactlyTenCoinsLeavesZero() = runTest {
        val store = FakeStore(prefs(Progress(0, 10, emptySet(), emptySet(), emptySet(), 0)))
        val h = start(store)
        h.vm.onHintLetter()
        assertEquals(0, h.vm.ready().coins)
        h.vm.onHintLetter()
        assertEquals(0, h.vm.ready().coins)
        assertEquals(GameEvent.NotEnoughCoins, h.events.last())
    }

    @Test
    fun hintWordWhenAllCellsRevealedStillWorksAndHintLetterIsFree() = runTest {
        val store = FakeStore(prefs(Progress(0, 1000, emptySet(), emptySet(), emptySet(), 0)))
        val h = start(store)
        val v = h.vm
        repeat(5) { v.onHintLetter() }
        assertEquals(950, v.ready().coins)
        v.onHintLetter()
        assertEquals(950, v.ready().coins)
        assertEquals(GameEvent.NoHintAvailable, h.events.last())
        assertEquals(2, v.ready().remainingWords) // harf açmak kelime bulmuş saymaz
        v.onHintWord()
        assertEquals(920, v.ready().coins)
    }

    // ---------- bonus ----------

    @Test
    fun restoredMeterAndBonusWordsAreRespectedNoDoubleCount() = runTest {
        val store = FakeStore(prefs(Progress(0, 50, emptySet(), setOf("et", "en", "te"), emptySet(), 3)))
        val h = start(store)
        val v = h.vm
        v.tap(1, 0); v.onSubmit() // "et" zaten bonus bulundu
        assertEquals(GameEvent.Submitted(SubmitResult.AlreadyFound("et")), h.events.last())
        assertEquals(3, v.ready().bonusMeter)
        assertEquals(50, v.ready().coins)
        v.tap(1, 2); v.onSubmit() // "en"... zaten
        v.tap(2, 1); v.onSubmit() // "ne" yeni bonus: 4
        assertEquals(4, v.ready().bonusMeter)
        v.tap(1, 2, 0); v.onSubmit() // "ent": 5 -> +20
        assertEquals(0, v.ready().bonusMeter)
        assertEquals(70, v.ready().coins)
        assertEquals(1, h.events.count { it == GameEvent.BonusMeterFull })
        assertEquals(1, h.events.count { it == GameEvent.CoinsEarned(20) })
    }

    @Test
    fun absurdSavedMeterIsClamped() = runTest {
        val store = FakeStore(prefs(Progress(0, 50, emptySet(), emptySet(), emptySet(), 1_000_000)))
        val h = start(store)
        assertEquals(4, h.vm.ready().bonusMeter)
        h.vm.tap(1, 0); h.vm.onSubmit()
        assertEquals(0, h.vm.ready().bonusMeter)
        assertEquals(70, h.vm.ready().coins)
    }

    @Test
    fun bonusMeterSurvivesNextLevel() = runTest {
        val h = start()
        val v = h.vm
        v.tap(1, 0); v.onSubmit() // et
        v.tap(0, 1, 2); v.onSubmit(); v.tap(2, 1, 0); v.onSubmit()
        v.onNextLevel()
        assertEquals(1, v.ready().bonusMeter)
        assertEquals(2, v.ready().levelNumber)
    }

    // ---------- kayıt kanalı ----------

    @Test
    fun conflatedSavesKeepLastStateEvenWhenConsumerIsStalled() = runTest {
        val store = FakeStore()
        val h = start(store)
        val v = h.vm
        // Tüketici çalışmıyor (StandardTestDispatcher): peş peşe çok değişiklik
        v.tap(0, 1, 2); v.onSubmit()
        v.tap(1, 0); v.onSubmit()
        v.onHintLetter()
        v.tap(2, 1, 0); v.onSubmit()
        v.onNextLevel()
        v.tap(1, 0); v.onSubmit()
        val expected = v.ready()
        runCurrent()
        val saved = ProgressRepository(store).progress.first()
        assertEquals(1, saved.levelIndex)
        assertEquals(expected.coins, saved.coins)
        assertEquals(expected.bonusMeter, saved.bonusMeter)
    }

    @Test
    fun onClearedFlushesPendingSave() = runTest {
        val store = FakeStore()
        val h = start(store)
        h.vm.tap(0, 1, 2); h.vm.onSubmit()
        clear(h.vm) // runCurrent çağrılmadan kapat
        runCurrent()
        val saved = ProgressRepository(store).progress.first()
        assertEquals(setOf("ten"), saved.found)
    }

    @Test
    fun failingSaveDoesNotKillConsumerAndNextSaveCarriesLatest() = runTest {
        val store = FakeStore(failNextUpdates = 1)
        val h = start(store)
        val v = h.vm
        v.tap(0, 1, 2); v.onSubmit()
        runCurrent() // ilk kayıt IOException
        assertEquals(emptySet<String>(), ProgressRepository(store).progress.first().found)
        v.tap(2, 1, 0); v.onSubmit()
        runCurrent()
        assertEquals(setOf("ten", "net"), ProgressRepository(store).progress.first().found)
        assertEquals(60, ProgressRepository(store).progress.first().coins)
    }

    // ---------- yükleme / bozuk veri ----------

    @Test
    fun actionsBeforeLoadAreIgnoredWithoutCrash() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        val vm = GameViewModel(levels(), { dict }, ProgressRepository(FakeStore()), Random(1), d)
        assertEquals(GameUiState.Loading, vm.state.value)
        vm.onLetter(0); vm.onDelete(); vm.onClear(); vm.onSubmit(); vm.onShuffle()
        vm.onHintLetter(); vm.onHintWord(); vm.onNextLevel()
        assertEquals(GameUiState.Loading, vm.state.value)
        runCurrent()
        val s = vm.ready()
        assertEquals("", s.currentWord)
        assertEquals(50, s.coins)
        assertEquals(1, s.levelNumber)
    }

    @Test
    fun nonIoFailureWhileReadingProgressGivesErrorState() = runTest {
        val bad = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IllegalStateException("kötü") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IllegalStateException()
        }
        val d = StandardTestDispatcher(testScheduler)
        val vm = GameViewModel(levels(), { dict }, ProgressRepository(bad), Random(1), d)
        runCurrent()
        assertTrue(vm.state.value is GameUiState.Error)
    }

    @Test
    fun ioFailureWhileReadingProgressFallsBackToDefaults() = runTest {
        val bad = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("okunamadı") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException()
        }
        val d = StandardTestDispatcher(testScheduler)
        val vm = GameViewModel(levels(), { dict }, ProgressRepository(bad), Random(1), d)
        runCurrent()
        assertEquals(50, vm.ready().coins)
    }

    @Test
    fun staleForeignSavedDataDoesNotBreakLevel() = runTest {
        val p = Progress(
            0, 50, setOf("ala", "zzz", "ten"), setOf("qqq"), setOf(99 to 99, 0 to 0), 0,
        )
        val h = start(FakeStore(prefs(p)))
        val s = h.vm.ready()
        assertEquals(1, s.remainingWords) // yalnızca "ten" geçerli
        assertEquals(listOf(true, false), s.wordRows.map { it.found })
        h.vm.onHintLetter()
        assertTrue(h.vm.ready().coins in 40..50)
        h.vm.tap(2, 1, 0); h.vm.onSubmit()
        assertTrue(h.vm.ready().levelComplete)
    }

    @Test
    fun lastLevelCompletionAllDoneAndReopen() = runTest {
        val store = FakeStore(prefs(Progress(2, 15, emptySet(), emptySet(), emptySet(), 2)))
        val h = start(store)
        val v = h.vm
        v.tap(0, 1, 2); v.onSubmit()
        v.tap(0, 1); v.onSubmit() // bonus: "al"
        assertEquals(3, v.ready().bonusMeter)
        v.onHintWord() // 15 jeton, yetmez
        assertEquals(15, v.ready().coins)
        v.tap(1, 2); v.onSubmit() // "la" izgara kelimesi
        assertTrue(v.ready().levelComplete)
        assertEquals(25, v.ready().coins)
        v.onNextLevel()
        assertEquals(GameUiState.AllDone, v.state.value)
        assertEquals(1, h.events.count { it == GameEvent.AllLevelsDone })
        v.onNextLevel()
        assertEquals(1, h.events.count { it == GameEvent.AllLevelsDone })
        runCurrent()
        val saved = ProgressRepository(store).progress.first()
        assertEquals(3, saved.levelIndex)
        assertEquals(25, saved.coins)
        assertEquals(3, saved.bonusMeter)
        val h2 = start(store)
        assertEquals(GameUiState.AllDone, h2.vm.state.value)
    }

    // ---------- eşzamanlılık ----------

    @Test
    fun concurrentHammeringKeepsInvariants() = runTest {
        val store = FakeStore(prefs(Progress(0, 100000, emptySet(), emptySet(), emptySet(), 0)))
        val h = start(store, seed = 3)
        val v = h.vm
        val errors = java.util.concurrent.CopyOnWriteArrayList<Throwable>()
        val threads = (1..6).map { t ->
            Thread {
                try {
                    val r = Random(t)
                    repeat(3000) {
                        when (r.nextInt(8)) {
                            0, 1, 2 -> v.onLetter(r.nextInt(-1, 5))
                            3 -> v.onDelete()
                            4 -> v.onSubmit()
                            5 -> v.onShuffle()
                            6 -> v.onHintLetter()
                            else -> if (r.nextInt(10) == 0) v.onHintWord() else v.onClear()
                        }
                        val s = v.state.value
                        if (s is GameUiState.Ready) checkReady(s)
                    }
                } catch (e: Throwable) { errors += e }
            }.also { it.start() }
        }
        threads.forEach { it.join() }
        assertTrue("hatalar: $errors", errors.isEmpty())
    }

    private fun checkReady(s: GameUiState.Ready) {
        val n = s.tiles.size
        assertEquals((0 until n).toSet(), s.tiles.map { it.index }.toSet())
        assertEquals(s.currentWord.length, s.tiles.count { it.used })
        assertEquals(s.currentWord.toList().sorted(), s.tiles.filter { it.used }.map { it.char }.sorted())
        assertTrue(s.coins >= 0)
        assertEquals(s.remainingWords == 0, s.levelComplete)
        assertEquals(s.remainingWords, s.wordRows.count { !it.found })
        assertTrue(s.bonusMeter in 0 until s.bonusTarget)
    }

    /**
     * Rastgele eylem dizisi + rastgele yeniden başlatma. Her adımda:
     * jeton farkı = kazanılan(CoinsEarned) - ipucu bedeli; HintUsed yoksa bedel 0; değişmezler korunur;
     * her seviye için LevelCompleted en çok bir kez; yeniden açılışta durum aynı.
     */
    @Test
    fun monkeyWithRestartsPreservesAccountingAndState() = runTest {
        repeat(60) { seed ->
            val r = Random(seed)
            val store = FakeStore(prefs(Progress(0, r.nextInt(0, 120), emptySet(), emptySet(), emptySet(), r.nextInt(0, 5))))
            var h = start(store, seed = seed)
            val paid = HashMap<Int, Int>() // levelNumber -> LevelCompleted sayısı
            var steps = 0
            while (steps++ < 250 && h.vm.state.value is GameUiState.Ready) {
                val v = h.vm
                val before = v.ready()
                val n = h.events.size
                var cost = 0
                when (r.nextInt(11)) {
                    0, 1, 2, 3 -> v.onLetter(r.nextInt(-1, 4))
                    4 -> v.onDelete()
                    5 -> if (!before.levelComplete) v.onSubmit() // tamamlanmış seviyede onSubmit: ayrı BUG testi
                    6 -> v.onShuffle()
                    7 -> { v.onHintLetter(); cost = 10 }
                    8 -> { v.onHintWord(); cost = 30 }
                    9 -> v.onNextLevel()
                    else -> { // yeniden başlat
                        runCurrent(); clear(v); runCurrent()
                        h = start(store, seed = seed + 1000)
                        val a = h.vm.state.value
                        if (a is GameUiState.Ready) {
                            assertEquals(before.levelNumber, a.levelNumber)
                            assertEquals(before.coins, a.coins)
                            assertEquals(before.bonusMeter, a.bonusMeter)
                            assertEquals(before.remainingWords, a.remainingWords)
                            assertEquals(before.wordRows, a.wordRows)
                        }
                        continue
                    }
                }
                val new = h.events.drop(n)
                val after = v.state.value
                if (after is GameUiState.Ready) {
                    checkReady(after)
                    val earned = new.filterIsInstance<GameEvent.CoinsEarned>().sumOf { it.amount }
                    val spent = if (new.any { it is GameEvent.HintUsed }) cost else 0
                    if (after.levelNumber == before.levelNumber) {
                        assertEquals("seed $seed adım $steps $new", before.coins + earned - spent, after.coins)
                    }
                    paid.merge(before.levelNumber, new.count { it == GameEvent.LevelCompleted() }, Int::plus)
                    assertTrue("seed $seed seviye iki kez ödendi", paid.values.all { it <= 1 })
                } else {
                    break
                }
                if (new.count { it == GameEvent.LevelCompleted() } > 1) throw AssertionError("çift LevelCompleted")
            }
        }
    }
}
