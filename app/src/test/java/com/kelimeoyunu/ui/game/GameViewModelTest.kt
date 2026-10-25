package com.kelimeoyunu.ui.game

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.domain.WordDictionary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private lateinit var dir: File
    private val scopes = ArrayList<CoroutineScope>()
    private lateinit var dispatcher: kotlinx.coroutines.CoroutineDispatcher

    /** Gövde standart dispatcher'da; VM ve olay toplayıcı aynı zamanlayıcıdaki Unconfined'da (anında çalışır). */
    private fun test(block: suspend TestScope.() -> Unit) = runTest {
        dispatcher = UnconfinedTestDispatcher(testScheduler)
        block()
    }

    @Before
    fun setUp() {
        dir = kotlin.io.path.createTempDirectory("vm").toFile()
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        dir.deleteRecursively()
    }

    private fun gz(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }

    private val twoLevels = "1|ten|ten,0,0,y;net,0,2,d\n2|etm|met,0,0,y;tem,0,2,d\n"
    private fun levels(text: String = twoLevels) = LevelRepository { ByteArrayInputStream(gz(text)) }
    private val dict = WordDictionary(setOf("ten", "net", "met", "tem", "et", "en", "te", "ne", "ent"))

    private fun repo(): ProgressRepository {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return ProgressRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "p.preferences_pb") })
    }

    /** Yükleme DataStore (gerçek IO) okuduğu için Loading'den çıkmasını bekler. */
    private suspend fun vm(
        r: ProgressRepository,
        lv: LevelRepository = levels(),
        d: () -> WordDictionary = { dict },
    ): GameViewModel {
        val v = GameViewModel(lv, d, r, Random(1), dispatcher)
        withContext(Dispatchers.Default) { v.state.first { it !is GameUiState.Loading } }
        return v
    }

    private fun TestScope.events(vm: GameViewModel): List<GameEvent> {
        val list = ArrayList<GameEvent>()
        backgroundScope.launch(dispatcher) { vm.events.collect { list += it } }
        return list
    }

    private fun GameViewModel.ready() = state.value as GameUiState.Ready

    private fun GameViewModel.spell(word: String) {
        for (c in word) {
            val t = ready().tiles.first { it.char == c && !it.used }
            onLetter(t.index)
        }
    }

    /**
     * Kaydın DataStore'a yazılmasını bekler. Bazen dinleyen akış güncellemeyi kaçırdığından
     * (DataStore 1.1 davranışı) her turda taze okuma yapılır; gerçek zamanda 10 sn sınırı vardır.
     */
    private suspend fun ProgressRepository.awaitProgress(pred: (Progress) -> Boolean) =
        withContext(Dispatchers.Default) {
            withTimeout(10_000) {
                var p = progress.first()
                while (!pred(p)) {
                    delay(10)
                    p = progress.first()
                }
                p
            }
        }

    @Test
    fun startsReadyWithDefaults() = test {
        val v = vm(repo())
        val s = v.ready()
        assertEquals(1, s.levelNumber)
        assertEquals(50, s.coins)
        assertEquals(2, s.remainingWords)
        assertEquals(0, s.bonusMeter)
        assertEquals(5, s.bonusTarget)
        assertEquals("", s.currentWord)
        assertEquals(listOf('t', 'e', 'n'), s.tiles.map { it.char })
        assertEquals(listOf(0, 1, 2), s.tiles.map { it.index })
        assertTrue(s.tiles.none { it.used })
        assertEquals(listOf(3, 3), s.wordRows.map { it.length })
        assertEquals(listOf(1, 2), s.wordRows.map { it.number })
        assertTrue(s.wordRows.all { r -> r.cells.all { it == null } && !r.found })
        assertFalse(s.levelComplete)
    }

    @Test
    fun letterByIndexAndNoDoubleSelection() = test {
        val v = vm(repo())
        val ev = events(v)
        v.onLetter(2)
        v.onLetter(2)
        v.onLetter(0)
        assertEquals("nt", v.ready().currentWord)
        assertEquals(listOf(true, false, true), v.ready().tiles.map { it.used })
        assertEquals(listOf(GameEvent.LetterAdded('n', 2), GameEvent.LetterAdded('t', 0)), ev)
        v.onLetter(-1)
        v.onLetter(99)
        assertEquals("nt", v.ready().currentWord)
    }

    @Test
    fun deleteAndClear() = test {
        val v = vm(repo())
        val ev = events(v)
        v.onDelete()
        assertEquals("", v.ready().currentWord)
        v.spell("ten")
        v.onDelete()
        assertEquals("te", v.ready().currentWord)
        assertFalse(v.ready().tiles.first { it.char == 'n' }.used)
        v.onClear()
        assertEquals("", v.ready().currentWord)
        assertTrue(v.ready().tiles.none { it.used })
        assertTrue(GameEvent.Deleted in ev && GameEvent.Cleared in ev)
        v.onClear() // boşken çökmez
    }

    @Test
    fun emptySubmitDoesNotChangeState() = test {
        val v = vm(repo())
        val ev = events(v)
        val before = v.state.value
        v.onSubmit()
        assertEquals(before, v.state.value)
        assertEquals(listOf<GameEvent>(GameEvent.Submitted(SubmitResult.Empty)), ev)
    }

    @Test
    fun correctWordClearsInputAndFreesTiles() = test {
        val v = vm(repo())
        val ev = events(v)
        v.spell("ten")
        v.onSubmit()
        val s = v.ready()
        assertEquals("", s.currentWord)
        assertTrue(s.tiles.none { it.used })
        assertEquals(1, s.remainingWords)
        assertEquals(listOf<Char?>('t', 'e', 'n'), s.wordRows[0].cells)
        assertTrue(s.wordRows[0].found)
        // net: ilk harf n, (0,2) kesişimi açık
        assertEquals(listOf('n', null, null), s.wordRows[1].cells)
        assertFalse(s.wordRows[1].found)
        assertTrue(GameEvent.Submitted(SubmitResult.Correct("ten")) in ev)
        assertEquals(50, s.coins)
    }

    @Test
    fun alreadyFoundInvalidAndBonusFlow() = test {
        val v = vm(repo())
        val ev = events(v)
        v.spell("ten"); v.onSubmit()
        v.spell("ten"); v.onSubmit()
        assertEquals(GameEvent.Submitted(SubmitResult.AlreadyFound("ten")), ev.last())
        assertEquals("", v.ready().currentWord)
        v.spell("nt"); v.onSubmit()
        assertEquals(GameEvent.Submitted(SubmitResult.Invalid("nt")), ev.last())
        v.onClear()
        v.spell("et"); v.onSubmit()
        assertEquals(GameEvent.Submitted(SubmitResult.Bonus("et")), ev.last())
        assertEquals(1, v.ready().bonusMeter)
        assertEquals("", v.ready().currentWord)
        assertEquals(1, v.ready().remainingWords)
    }

    @Test
    fun doubleSubmitCountsOnce() = test {
        val v = vm(repo())
        val ev = events(v)
        v.spell("ten")
        v.onSubmit()
        v.onSubmit()
        assertEquals(
            listOf<GameEvent>(
                GameEvent.LetterAdded('t', 0), GameEvent.LetterAdded('e', 1), GameEvent.LetterAdded('n', 2),
                GameEvent.Submitted(SubmitResult.Correct("ten")),
                GameEvent.Submitted(SubmitResult.Empty),
            ),
            ev,
        )
        assertEquals(1, v.ready().remainingWords)
    }

    @Test
    fun fifthBonusGivesTwentyCoinsAndResetsMeter() = test {
        val v = vm(repo())
        val ev = events(v)
        listOf("et", "en", "te", "ne").forEach { v.spell(it); v.onSubmit() }
        assertEquals(4, v.ready().bonusMeter)
        assertEquals(50, v.ready().coins)
        assertFalse(GameEvent.BonusMeterFull in ev)
        v.spell("ent"); v.onSubmit()
        assertEquals(0, v.ready().bonusMeter)
        assertEquals(70, v.ready().coins)
        val tail = ev.takeLast(3)
        assertEquals(GameEvent.Submitted(SubmitResult.Bonus("ent")), tail[0])
        assertTrue(GameEvent.BonusMeterFull in tail && GameEvent.CoinsEarned(20) in tail)
    }

    @Test
    fun notEnoughCoinsDoesNotChargeOrHint() = test {
        val r = repo()
        r.save(Progress(0, 5, emptySet(), emptySet(), emptySet(), 0))
        val v = vm(r)
        val ev = events(v)
        v.onHintLetter()
        v.onHintWord()
        assertEquals(listOf<GameEvent>(GameEvent.NotEnoughCoins, GameEvent.NotEnoughCoins), ev)
        assertEquals(5, v.ready().coins)
        assertEquals(2, v.ready().remainingWords)
    }

    @Test
    fun hintCostsAndEffects() = test {
        val v = vm(repo())
        val ev = events(v)
        v.onHintLetter()
        assertEquals(40, v.ready().coins)
        assertEquals(1, v.ready().wordRows.sumOf { r -> r.cells.count { it != null } })
        assertTrue(GameEvent.HintUsed in ev)
        v.onHintWord()
        assertEquals(10, v.ready().coins)
        assertEquals(1, v.ready().remainingWords)
        v.onHintWord() // 10 < 30
        assertEquals(10, v.ready().coins)
        assertEquals(GameEvent.NotEnoughCoins, ev.last())
    }

    @Test
    fun hintLetterWithNothingToRevealCostsNothing() = test {
        val r = repo()
        r.save(Progress(0, 100, emptySet(), emptySet(), emptySet(), 0))
        val v = vm(r)
        val ev = events(v)
        repeat(5) { v.onHintLetter() } // ızgarada 5 kutu
        assertEquals(50, v.ready().coins)
        v.onHintLetter()
        assertEquals(50, v.ready().coins)
        assertEquals(GameEvent.NoHintAvailable, ev.last())
    }

    @Test
    fun levelCompletionGivesTenCoinsOnce() = test {
        val v = vm(repo())
        val ev = events(v)
        v.spell("ten"); v.onSubmit()
        assertFalse(v.ready().levelComplete)
        v.spell("net"); v.onSubmit()
        assertTrue(v.ready().levelComplete)
        assertEquals(60, v.ready().coins)
        assertTrue(GameEvent.LevelCompleted in ev && GameEvent.CoinsEarned(10) in ev)
        v.onHintWord(); v.onHintLetter()
        assertEquals(60, v.ready().coins)
    }

    @Test
    fun hintWordCanCompleteLevel() = test {
        val r = repo()
        r.save(Progress(0, 100, emptySet(), emptySet(), emptySet(), 0))
        val v = vm(r)
        val ev = events(v)
        v.onHintWord(); v.onHintWord()
        assertTrue(v.ready().levelComplete)
        assertEquals(100 - 60 + 10, v.ready().coins)
        assertTrue(GameEvent.LevelCompleted in ev)
    }

    @Test
    fun nextLevelAndAllDoneAfterLast() = test {
        val r = repo()
        val v = vm(r)
        val ev = events(v)
        v.onNextLevel() // bitmeden etkisiz
        assertEquals(1, v.ready().levelNumber)
        v.spell("ten"); v.onSubmit(); v.spell("net"); v.onSubmit()
        v.onNextLevel()
        val s = v.ready()
        assertEquals(2, s.levelNumber)
        assertEquals(2, s.remainingWords)
        assertEquals(listOf('e', 't', 'm').sorted(), s.tiles.map { it.char }.sorted())
        assertFalse(s.levelComplete)
        v.spell("met"); v.onSubmit(); v.spell("tem"); v.onSubmit()
        assertTrue(v.ready().levelComplete)
        v.onNextLevel()
        assertEquals(GameUiState.AllDone, v.state.value)
        assertEquals(GameEvent.AllLevelsDone, ev.last())
        v.onNextLevel(); v.onSubmit(); v.onLetter(0); v.onHintLetter() // çökmez
        r.awaitProgress { it.levelIndex == 2 }
        assertEquals(GameUiState.AllDone, vm(r).state.value)
    }

    @Test
    fun progressRoundTripAfterRestart() = test {
        val r = repo()
        val v = vm(r)
        v.spell("ten"); v.onSubmit()
        v.spell("et"); v.onSubmit()
        v.onHintLetter()
        val before = v.ready()
        r.awaitProgress { it.coins == 40 && it.revealed.size == 1 }
        val s = vm(r).ready()
        assertEquals(before.levelNumber, s.levelNumber)
        assertEquals(before.coins, s.coins)
        assertEquals(before.bonusMeter, s.bonusMeter)
        assertEquals(before.remainingWords, s.remainingWords)
        assertEquals(before.wordRows, s.wordRows)
    }

    @Test
    fun restartAfterLevelAdvanceRestoresNewLevel() = test {
        val r = repo()
        val v = vm(r)
        v.spell("ten"); v.onSubmit(); v.spell("net"); v.onSubmit()
        v.onNextLevel()
        r.awaitProgress { it.levelIndex == 1 }
        val s = vm(r).ready()
        assertEquals(2, s.levelNumber)
        assertEquals(60, s.coins)
        assertEquals(2, s.remainingWords)
    }

    @Test
    fun restartOnCompletedLevelDoesNotPayAgain() = test {
        val r = repo()
        val v = vm(r)
        v.spell("ten"); v.onSubmit(); v.spell("net"); v.onSubmit()
        r.awaitProgress { it.coins == 60 && it.found.size == 2 }
        val s = vm(r).ready()
        assertTrue(s.levelComplete)
        assertEquals(60, s.coins)
    }

    @Test
    fun shuffleKeepsLettersAndCurrentWord() = test {
        val v = vm(repo())
        val ev = events(v)
        v.onLetter(0)
        repeat(5) {
            v.onShuffle()
            val s = v.ready()
            assertEquals("t", s.currentWord)
            assertEquals(listOf('e', 'n', 't'), s.tiles.map { it.char }.sorted())
            assertEquals(setOf(0, 1, 2), s.tiles.map { it.index }.toSet())
            assertEquals(listOf(0), s.tiles.filter { it.used }.map { it.index })
        }
        assertEquals(5, ev.count { it == GameEvent.Shuffled })
        v.onLetter(2) // karıştırmadan sonra da index kimliği geçerli
        assertEquals("tn", v.ready().currentWord)
    }

    @Test
    fun emptyLevelFileGivesError() = test {
        val v = vm(repo(), levels(""))
        assertTrue(v.state.value is GameUiState.Error)
        v.onLetter(0); v.onSubmit(); v.onShuffle(); v.onNextLevel(); v.onHintLetter() // çökmez
    }

    @Test
    fun garbageLevelFileGivesError() = test {
        val v = vm(repo(), LevelRepository { ByteArrayInputStream("gzip değil".toByteArray()) })
        assertTrue(v.state.value is GameUiState.Error)
    }

    @Test
    fun missingLevelAssetGivesError() = test {
        val v = vm(repo(), LevelRepository { throw IOException("yok") })
        assertTrue(v.state.value is GameUiState.Error)
    }

    @Test
    fun dictionaryFailureGivesError() = test {
        val v = vm(repo(), d = { throw IOException("sözlük yok") })
        val s = v.state.value
        assertTrue(s is GameUiState.Error && s.message.isNotBlank())
    }

    @Test
    fun savedLevelIndexBeyondEndGivesAllDone() = test {
        val r = repo()
        r.save(Progress(99, 10, emptySet(), emptySet(), emptySet(), 0))
        assertEquals(GameUiState.AllDone, vm(r).state.value)
    }
}
