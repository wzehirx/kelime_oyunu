package com.kelimeoyunu.ui.game

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kelimeoyunu.TestPlaces
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.PlaceState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QA: gerçek WorldTour (100 ülke / 10.000 seviye) ile rastgele uzun oyun simülasyonu, kapat-aç ve liste durumu.
 * Her seviye: letters "teen", ızgara TEN/NET, bonus kelimeler TEEN/TEE/NEE.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorldTourSimulationAdversarialTest {
    private lateinit var dir: File
    private lateinit var dispatcher: CoroutineDispatcher
    private val scopes = ArrayList<CoroutineScope>()
    private val total = 10_000
    private val tour = WorldTour(TestPlaces.list(100), total)
    private val dict = WordDictionary(setOf("ten", "net", "teen", "tee", "nee"))
    private val bonusWords = listOf("teen", "tee", "nee")
    private val gz: ByteArray by lazy {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { o ->
            o.write((1..total).joinToString("") { "$it|teen|ten,0,0,y;net,0,2,d\n" }.toByteArray())
        }
        out.toByteArray()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(timeout = kotlin.time.Duration.parse("300s")) {
        dispatcher = UnconfinedTestDispatcher(testScheduler)
        block()
    }

    @Before fun setUp() { dir = kotlin.io.path.createTempDirectory("sim").toFile() }
    @After fun tearDown() { scopes.forEach { it.cancel() }; dir.deleteRecursively() }

    private fun repo(name: String = "p.preferences_pb"): ProgressRepository {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return ProgressRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, name) })
    }

    private fun newVm(r: ProgressRepository) =
        GameViewModel(LevelRepository { ByteArrayInputStream(gz) }, { dict }, r, Random(1), dispatcher, { tour })

    // Ready'yi gören iş parçacığı, olayı işleyen birleşmemiş (unconfined) toplayıcıdan önce davranabilir; kısa bekleme yarışı kapatır.
    private suspend fun GameViewModel.awaitReady() = withContext(Dispatchers.Default) {
        state.first { it !is GameUiState.Loading }.also { delay(50) }
    }
    private fun TestScope.collect(vm: GameViewModel): MutableList<GameEvent> {
        val list = ArrayList<GameEvent>()
        backgroundScope.launch(dispatcher) { vm.events.collect { list += it } }
        return list
    }
    private fun GameViewModel.ready() = state.value as GameUiState.Ready
    private fun GameViewModel.spell(word: String) {
        for (c in word) onLetter(ready().tiles.first { it.char == c && !it.used }.index)
        onSubmit()
    }
    private fun GameViewModel.finishLevel() { spell("ten"); spell("net") }
    private suspend fun ProgressRepository.await(pred: (Progress) -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(20_000) { var p = progress.first(); while (!pred(p)) { delay(10); p = progress.first() }; p }
    }
    private suspend fun seed(r: ProgressRepository, idx: Int, coins: Int = 500, meter: Int = 0, found: Set<String> = emptySet()) =
        r.save(Progress(idx, coins, found, emptySet(), emptySet(), meter))

    /** Rastgele oyun: [from] seviyeden [to] seviyeye (dahil değil) kadar oynar; jeton modelini her seviyede doğrular. */
    private suspend fun TestScope.simulate(seedValue: Int, from: Int, to: Int) {
        val r = repo()
        seed(r, from)
        val vm = newVm(r)
        val ev = collect(vm)
        vm.awaitReady()
        val rng = Random(seedValue)
        var coins = 500
        var meter = 0
        var level = from
        while (level < to) {
            val before = vm.ready()
            assertEquals("seviye numarası", level + 1, before.levelNumber)
            assertEquals("seviye başı jeton, level=$level", coins, before.coins)
            when (rng.nextInt(5)) {
                1 -> repeat(rng.nextInt(1, 4)) { k ->
                    vm.spell(bonusWords[k])
                    meter++
                    if (meter == 5) { meter = 0; coins += 20 }
                }
                2 -> if (coins >= 30) { vm.onHintWord(); coins -= 30 }
                3 -> { vm.onLetter(0); vm.onShuffle(); vm.onClear(); vm.onDelete() }
                4 -> { vm.onNextLevel() /* tamamlanmamış seviyede etkisiz olmalı */ }
            }
            vm.finishLevel()
            coins += 10
            val pos = tour.positionOf(level)!!
            if (pos.levelInPlace == pos.placeLevelCount) coins += 25
            val done = vm.ready()
            assertTrue("seviye $level bitti", done.levelComplete)
            assertEquals("jeton, level=$level place=${pos.placeIndex}/${pos.levelInPlace}", coins, done.coins)
            // yinelenen girişler ödülü çoğaltmaz
            vm.onHintWord(); vm.onHintLetter(); vm.onSubmit(); vm.spell("ten")
            assertEquals(coins, vm.ready().coins)
            vm.onNextLevel()
            if (rng.nextBoolean()) vm.onNextLevel() // çift basış
            level++
            if (level < total) assertEquals(level + 1, vm.ready().levelNumber) else assertEquals(GameUiState.AllDone, vm.state.value)
        }
        // olay denetimi
        val entered = HashMap<Int, Int>(); val completed = HashMap<Int, Int>()
        val order = ArrayList<Pair<String, Int>>()
        ev.forEachIndexed { i, e ->
            when (e) {
                is GameEvent.PlaceEntered -> {
                    val idx = tour.positionOf(0).let { _ -> (0 until 100).first { tour.place(it) == e.place } }
                    entered.merge(idx, 1, Int::plus); order += "E" to idx
                    assertEquals(tour.levelCountOf(idx), e.placeLevelCount)
                }
                is GameEvent.PlaceCompleted -> {
                    val idx = (0 until 100).first { tour.place(it) == e.place }
                    completed.merge(idx, 1, Int::plus); order += "C" to idx
                    assertEquals(25, e.bonusCoins)
                    assertEquals(tour.place(idx + 1)?.ad, e.nextPlaceName)
                    assertEquals("PlaceCompleted'dan önce LevelCompleted", GameEvent.LevelCompleted, ev[i - 1])
                    assertEquals("ondan önce CoinsEarned(10)", GameEvent.CoinsEarned(10), ev[i - 2])
                }
                else -> Unit
            }
        }
        val firstPlace = tour.positionOf(from)!!
        val lastPlace = tour.positionOf(to - 1)!!
        for (p in firstPlace.placeIndex..lastPlace.placeIndex) {
            val startsInRange = tour.startIndexOf(p) >= from
            val endsInRange = tour.startIndexOf(p) + tour.levelCountOf(p) <= to
            assertEquals("ülke $p giriş sayısı", if (startsInRange) 1 else 0, entered[p] ?: 0)
            assertEquals("ülke $p bitiş sayısı", if (endsInRange) 1 else 0, completed[p] ?: 0)
        }
        // sıra: ülke p için E(p) < C(p) < E(p+1)
        val flat = order.map { (k, p) -> if (k == "E") 2 * p else 2 * p + 1 }
        assertEquals("olay sırası monoton", flat.sorted(), flat)
        // kayıt
        r.await { it.coins == coins && it.levelIndex == (if (level >= total) total else level) }
    }

    @Test fun randomGameSeed1CrossesFirstTwelveCountries() = test { simulate(1, 0, 20 * 10 + 50 * 2 + 7) }
    @Test fun randomGameSeed2CrossesCountriesAroundMidBoundary() = test { simulate(2, 195, 1710) }
    @Test fun randomGameSeed3RunsToTheVeryEndAndAllDone() = test { simulate(3, total - 300, total) }
    @Test fun randomGameSeed4StartsMidCountry() = test { simulate(4, 1000, 1400) }
    @Test fun fullTenThousandLevelGameSeed5() = test { simulate(5, 0, total) }

    // ---------- kapat-aç ----------

    @Test fun reopenMidCountryAfterLevelDoneBeforeNextGivesNoRepeats() = test {
        val r = repo(); seed(r, 7)
        val vm = newVm(r); collect(vm); vm.awaitReady()
        vm.finishLevel()
        r.await { it.coins == 510 }
        val vm2 = newVm(r); val ev2 = collect(vm2); vm2.awaitReady()
        assertTrue(vm2.ready().levelComplete)
        assertEquals(510, vm2.ready().coins)
        vm2.onNextLevel()
        assertEquals(510, vm2.ready().coins)
        assertTrue("olay yok: $ev2", ev2.none { it is GameEvent.PlaceEntered || it is GameEvent.PlaceCompleted || it is GameEvent.CoinsEarned })
    }

    @Test fun reopenAfterLastLevelOfCountryDoneBeforeNextPaysAndAnnouncesNothingTwice() = test {
        val r = repo(); seed(r, 19)
        val vm = newVm(r); collect(vm); vm.awaitReady()
        vm.finishLevel()
        r.await { it.coins == 535 }
        repeat(3) {
            val again = newVm(r); val ev = collect(again); again.awaitReady()
            assertEquals(535, again.ready().coins)
            assertTrue("kapat-aç #$it: $ev", ev.none { e -> e is GameEvent.PlaceCompleted || e is GameEvent.CoinsEarned || e is GameEvent.PlaceEntered })
        }
        val last = newVm(r); val ev = collect(last); last.awaitReady()
        last.onNextLevel(); last.onNextLevel()
        assertEquals(1, ev.count { e -> e is GameEvent.PlaceEntered })
        assertEquals(535, last.ready().coins)
    }

    @Test fun killedBetweenNextLevelAndSaveDoesNotLoseOrDoubleBonus() = test {
        // son kayıt: ülke bitti (index 19, tamamlanmış). Yeni VM + onNextLevel iki kez.
        val r = repo(); seed(r, 19, coins = 535, found = setOf("ten", "net"))
        val vm = newVm(r); val ev = collect(vm); vm.awaitReady()
        vm.onNextLevel(); vm.onNextLevel()
        assertEquals(535, vm.ready().coins)
        assertEquals(1, ev.count { it is GameEvent.PlaceEntered })
    }

    @Test fun BUG_reenteringGameAtFirstLevelOfCountryDoesNotReplayTheWholeIntro() = test {
        // Oyun ekranı her girişte yeni ViewModel kurar (AppRoot.GameRoute). Oyuncu ülkenin 1. seviyesindeyken
        // menüye çıkıp geri girerse "Yeni ülke ..." duyurusu + melodi her seferinde tekrar çalar.
        val r = repo(); seed(r, 20)
        val first = newVm(r); val ev1 = collect(first); first.awaitReady()
        assertEquals(1, ev1.count { it is GameEvent.PlaceEntered })
        val second = newVm(r); val ev2 = collect(second); second.awaitReady()
        assertEquals("aynı ülkeye ikinci girişte tanıtım tekrarlanmamalı", 0, ev2.count { it is GameEvent.PlaceEntered })
    }


    @Test fun stressFirstEntryEventIsNeverLostNorDoubledAcrossRaces() = test {
        var lost = 0; var doubled = 0
        repeat(300) { n ->
            val r = repo("s$n.preferences_pb"); seed(r, 20)
            val vm = newVm(r)
            val ev = ArrayList<GameEvent>()
            // dinleyici, yükleme ile yarışacak biçimde farklı anlarda katılır
            if (n % 3 == 0) backgroundScope.launch(dispatcher) { vm.events.collect { synchronized(ev) { ev += it } } }
            if (n % 3 == 1) { kotlinx.coroutines.yield() }
            val job = if (n % 3 != 0) backgroundScope.launch(Dispatchers.Default) { vm.events.collect { synchronized(ev) { ev += it } } } else null
            vm.awaitReady()
            withContext(Dispatchers.Default) { delay(5) }
            val c = synchronized(ev) { ev.count { it is GameEvent.PlaceEntered } }
            if (c == 0) lost++
            if (c > 1) doubled++
            job?.cancel()
        }
        assertEquals("kaybolan=$lost", 0, lost)
        assertEquals("çift=$doubled", 0, doubled)
    }

    // ---------- liste durumu ----------

    @Test fun BUG_worldListAfterLastLevelDoneButBeforeNextShowsCountryAsCompleted() = test {
        // Oyuncu ülkenin son seviyesini bitirdi (+25 jeton, "tamamlandı" duyurusu geldi) ama "Sonraki seviye"ye basmadı.
        val r = repo(); seed(r, 19)
        val vm = newVm(r); val ev = collect(vm); vm.awaitReady()
        vm.finishLevel()
        assertEquals(1, ev.count { it is GameEvent.PlaceCompleted })
        val saved = r.await { it.coins == 535 }
        val st = tour.placeState(0, saved.levelIndex)
        assertTrue("liste: $st, fakat oyun 'Türkiye tamamlandı' dedi ve ödül verdi", st is PlaceState.Completed)
        assertEquals("başlıkta tamamlanan ülke sayısı", 1, tour.completedPlaces(saved.levelIndex))
    }

    @Test fun listAfterNextLevelIsConsistent() = test {
        val r = repo(); seed(r, 19)
        val vm = newVm(r); collect(vm); vm.awaitReady()
        vm.finishLevel(); vm.onNextLevel()
        val saved = r.await { it.levelIndex == 20 }
        assertTrue(tour.placeState(0, saved.levelIndex) is PlaceState.Completed)
        assertEquals(PlaceState.InProgress(0, 20), tour.placeState(1, saved.levelIndex))
    }
}
