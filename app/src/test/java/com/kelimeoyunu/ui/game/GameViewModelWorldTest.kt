package com.kelimeoyunu.ui.game

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kelimeoyunu.TestPlaces
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.domain.WordDictionary
import com.kelimeoyunu.domain.WorldTour
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Dünya turu: ülke girişi/bitişi olayları, +25 jeton ödülü ve tur kapalıyken eski davranış. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelWorldTest {
    private lateinit var dir: File
    private lateinit var dispatcher: CoroutineDispatcher
    private val scopes = ArrayList<CoroutineScope>()

    // Her seviye aynı: TEN/NET. 3 ülke x 20 seviye = 60 seviye.
    private val levelText = (1..60).joinToString("") { "$it|ten|ten,0,0,y;net,0,2,d\n" }
    private val tour = WorldTour(TestPlaces.list(3), 60)
    private val dict = WordDictionary(setOf("ten", "net"))

    private fun test(block: suspend TestScope.() -> Unit) = runTest {
        dispatcher = UnconfinedTestDispatcher(testScheduler)
        block()
    }

    @Before fun setUp() { dir = kotlin.io.path.createTempDirectory("vmw").toFile() }
    @After fun tearDown() { scopes.forEach { it.cancel() }; dir.deleteRecursively() }

    private fun repo(name: String = "p.preferences_pb"): ProgressRepository {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return ProgressRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, name) })
    }

    private fun levels(): LevelRepository {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(levelText.toByteArray()) }
        return LevelRepository { ByteArrayInputStream(out.toByteArray()) }
    }

    private fun newVm(r: ProgressRepository, withTour: Boolean = true) =
        GameViewModel(levels(), { dict }, r, Random(1), dispatcher, { if (withTour) tour else null })

    private suspend fun GameViewModel.awaitReady() = withContext(Dispatchers.Default) { state.first { it !is GameUiState.Loading } }

    private fun TestScope.collect(vm: GameViewModel): MutableList<GameEvent> {
        val list = ArrayList<GameEvent>()
        backgroundScope.launch(dispatcher) { vm.events.collect { list += it } }
        return list
    }

    private fun GameViewModel.ready() = state.value as GameUiState.Ready

    private fun GameViewModel.spell(word: String) {
        for (c in word) onLetter(ready().tiles.first { it.char == c && !it.used }.index)
    }

    private fun GameViewModel.finishLevel() { spell("ten"); onSubmit(); spell("net"); onSubmit() }

    private suspend fun ProgressRepository.awaitProgress(pred: (Progress) -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(10_000) {
            var p = progress.first()
            while (!pred(p)) { delay(10); p = progress.first() }
            p
        }
    }

    private suspend fun seed(r: ProgressRepository, levelIndex: Int, coins: Int = 50, found: Set<String> = emptySet()) =
        r.save(Progress(levelIndex, coins, found, emptySet(), emptySet(), 0))

    private fun List<GameEvent>.places() = filter { it is GameEvent.PlaceEntered || it is GameEvent.PlaceCompleted }

    @Test fun freshGameAnnouncesTurkeyOnceWhetherListenerIsEarlyOrLate() = test {
        // dinleyici yükleme bitmeden (anlık yol)
        val early = newVm(repo("a.preferences_pb"))
        val earlyEvents = collect(early)
        early.awaitReady()
        assertEquals(listOf<GameEvent>(GameEvent.PlaceEntered(TestPlaces.turkey, 20)), earlyEvents.places())
        // dinleyici yükleme bittikten sonra (bekleyen olay yolu)
        val late = newVm(repo("b.preferences_pb"))
        late.awaitReady()
        val lateEvents = collect(late)
        assertEquals(listOf<GameEvent>(GameEvent.PlaceEntered(TestPlaces.turkey, 20)), lateEvents.places())
        val pos = late.ready().place!!
        assertEquals(Triple(0, 1, 20), Triple(pos.placeIndex, pos.levelInPlace, pos.placeLevelCount))
    }

    @Test fun secondListenerOnSameViewModelDoesNotHearItAgain() = test {
        val vm = newVm(repo())
        vm.awaitReady()
        val first = collect(vm)
        val second = collect(vm)
        assertEquals(1, first.places().size)
        assertTrue(second.places().isEmpty())
    }

    @Test fun resumingMidPlaceHasNoEntryEvent() = test {
        val r = repo(); seed(r, 5)
        val vm = newVm(r)
        val ev = collect(vm)
        vm.awaitReady()
        assertTrue(ev.places().isEmpty())
        assertEquals(6, vm.ready().place!!.levelInPlace)
    }

    @Test fun resumingAtFirstLevelOfLaterPlaceAnnouncesThatPlace() = test {
        val r = repo(); seed(r, 20)
        val vm = newVm(r)
        val ev = collect(vm)
        vm.awaitReady()
        assertEquals(listOf<GameEvent>(GameEvent.PlaceEntered(tour.place(1)!!, 20)), ev.places())
    }

    @Test fun finishingLastLevelOfPlaceGivesLevelThenPlaceCompletedAndBonusOnce() = test {
        val r = repo(); seed(r, 19)
        val vm = newVm(r)
        val ev = collect(vm)
        vm.awaitReady()
        ev.clear()
        vm.finishLevel()
        val tail = ev.dropWhile { it !is GameEvent.CoinsEarned }
        assertEquals(
            listOf(GameEvent.CoinsEarned(10, silent = true), GameEvent.LevelCompleted(placeCompleted = true), GameEvent.PlaceCompleted(TestPlaces.turkey, 25, "Ülke 1")),
            tail,
        )
        assertEquals(50 + 10 + 25, vm.ready().coins)
        assertEquals(85, r.awaitProgress { it.coins == 85 }.coins)

        // yinelenen işlemler ödülü tekrar vermez
        vm.onHintWord(); vm.onHintLetter(); vm.onSubmit()
        assertEquals(85, vm.ready().coins)
        vm.onNextLevel(); vm.onNextLevel()
        assertEquals(1, ev.count { it is GameEvent.PlaceCompleted })
        assertEquals(listOf<GameEvent>(GameEvent.PlaceEntered(tour.place(1)!!, 20)), ev.places().drop(1))
        assertEquals(85, vm.ready().coins)
        assertEquals(1, vm.ready().place!!.levelInPlace)
        assertEquals(1, vm.ready().place!!.placeIndex)
    }

    @Test fun reopeningCompletedLastLevelDoesNotPayAgain() = test {
        val r = repo(); seed(r, 19, coins = 85, found = setOf("ten", "net"))
        val vm = newVm(r)
        val ev = collect(vm)
        vm.awaitReady()
        assertTrue(vm.ready().levelComplete)
        vm.onNextLevel() // bitmiş seviyeden devam
        assertEquals(85, vm.ready().coins)
        assertTrue(ev.none { it is GameEvent.PlaceCompleted || it is GameEvent.CoinsEarned })
    }

    @Test fun hintWordCompletionAlsoCompletesPlace() = test {
        val r = repo(); seed(r, 19, coins = 100)
        val vm = newVm(r)
        val ev = collect(vm)
        vm.awaitReady()
        vm.onHintWord(); vm.onHintWord()
        assertEquals(100 - 60 + 10 + 25, vm.ready().coins)
        assertEquals(1, ev.count { it is GameEvent.PlaceCompleted })
    }

    @Test fun middleLevelsOfPlaceGiveOnlyTheLevelReward() = test {
        val r = repo(); seed(r, 7)
        val vm = newVm(r)
        val ev = collect(vm)
        vm.awaitReady()
        vm.finishLevel(); vm.onNextLevel()
        assertEquals(60, vm.ready().coins)
        assertTrue(ev.places().isEmpty())
        assertEquals(9, vm.ready().place!!.levelInPlace)
    }

    @Test fun lastPlaceCompletionHasNoNextNameAndKeepsAllDoneFlow() = test {
        val r = repo(); seed(r, 59)
        val vm = newVm(r)
        val ev = collect(vm)
        vm.awaitReady()
        vm.finishLevel()
        assertEquals(GameEvent.PlaceCompleted(tour.place(2)!!, 25, null), ev.last())
        vm.onNextLevel()
        assertEquals(GameUiState.AllDone, vm.state.value)
        assertEquals(GameEvent.AllLevelsDone, ev.last())
    }

    @Test fun withoutTourBehavesAsBefore() = test {
        val r = repo(); seed(r, 19)
        val vm = newVm(r, withTour = false)
        val ev = collect(vm)
        vm.awaitReady()
        assertNull(vm.ready().place)
        vm.finishLevel()
        assertEquals(60, vm.ready().coins)
        assertEquals(
            listOf(GameEvent.Submitted(SubmitResult.Correct("net")), GameEvent.CoinsEarned(10), GameEvent.LevelCompleted()),
            ev.takeLast(3),
        )
        vm.onNextLevel()
        assertNull(vm.ready().place)
        assertTrue(ev.places().isEmpty())
    }

    @Test fun failingTourProviderFallsBackToOldBehavior() = test {
        val vm = GameViewModel(levels(), { dict }, repo(), Random(1), dispatcher, { error("boom") })
        val ev = collect(vm)
        vm.awaitReady()
        assertNull(vm.ready().place)
        assertTrue(ev.places().isEmpty())
    }

    @Test fun introIsNotRepeatedAfterRebuildButPlaysAgainAfterReset() = test {
        val r = repo()
        val first = newVm(r); val ev1 = collect(first); first.awaitReady()
        assertEquals(1, ev1.count { it is GameEvent.PlaceEntered })
        r.introducedPlaceIndex.first { it == 0 }
        // menüye çık-gir ve "uygulamayı kapat-aç" aynı şeydir: yeni ViewModel
        repeat(2) {
            val again = newVm(r); val ev = collect(again); again.awaitReady()
            assertTrue("tekrar tanıtım: $ev", ev.none { e -> e is GameEvent.PlaceEntered })
        }
        r.reset()
        val fresh = newVm(r); val evFresh = collect(fresh); fresh.awaitReady()
        assertEquals(listOf<GameEvent>(GameEvent.PlaceEntered(TestPlaces.turkey, 20)), evFresh.places())
    }

    @Test fun nextCountryIntroIsPersistedSoRebuildAtItsFirstLevelStaysQuiet() = test {
        val r = repo(); seed(r, 19)
        val vm = newVm(r); collect(vm); vm.awaitReady()
        vm.finishLevel(); vm.onNextLevel()
        r.introducedPlaceIndex.first { it == 1 }
        r.awaitProgress { it.levelIndex == 20 }
        val again = newVm(r); val ev = collect(again); again.awaitReady()
        assertTrue(ev.none { it is GameEvent.PlaceEntered })
    }

    @Test fun middleOfCountryNeverIntroducesEvenWhenNothingIsRecorded() = test {
        val r = repo(); seed(r, 5)
        val vm = newVm(r); val ev = collect(vm); vm.awaitReady()
        assertTrue(ev.none { it is GameEvent.PlaceEntered })
    }

    @Test fun countryFinishIsSilentForCoinAndLevelJingleButNotForSpeech() = test {
        val r = repo(); seed(r, 19)
        val vm = newVm(r); val ev = collect(vm); vm.awaitReady()
        vm.finishLevel()
        assertTrue(ev.contains(GameEvent.CoinsEarned(10, silent = true)))
        assertTrue(ev.contains(GameEvent.LevelCompleted(placeCompleted = true)))
        val sfx = ev.flatMap { com.kelimeoyunu.audio.sfxFor(it) }
        assertTrue(sfx.toString(), com.kelimeoyunu.audio.Sfx.PLACE_COMPLETE in sfx)
        assertTrue(sfx.toString(), com.kelimeoyunu.audio.Sfx.LEVEL_COMPLETE !in sfx && com.kelimeoyunu.audio.Sfx.COIN !in sfx)
    }

    @Test fun ordinaryLevelFinishStillHasCoinAndLevelJingle() = test {
        val r = repo(); seed(r, 5)
        val vm = newVm(r); val ev = collect(vm); vm.awaitReady()
        vm.finishLevel()
        assertTrue(ev.contains(GameEvent.CoinsEarned(10)))
        assertTrue(ev.contains(GameEvent.LevelCompleted()))
    }
}
