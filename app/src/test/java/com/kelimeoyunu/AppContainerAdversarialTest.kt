package com.kelimeoyunu

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.ui.game.GameEvent
import com.kelimeoyunu.ui.game.GameUiState
import com.kelimeoyunu.ui.game.GameViewModel
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Gerçek (birleştirilmiş) asset'lerle. AGP `levels.txt.gz`'yi APK'ya düz `levels.txt` olarak koyar;
 * bu test o yedek yolunu ve 10.000. seviye sonrası akışı doğrular. (Tek sınıfta tek AppContainer:
 * aynı dosyada iki DataStore açılamaz.)
 */
@RunWith(RobolectricTestRunner::class)
class AppContainerAdversarialTest {

    @Test
    fun fallbackGzipMatchesRawAssetAndLastLevelFlowEndsInAllDone() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // 1) yedek yol: gzip akışı geçerli ve içerik ham asset ile aynı
        val viaFallback = GZIPInputStream(openLevelsGzip(app.assets)).use { it.readBytes() }
        val raw = app.assets.open("levels.txt").use { it.readBytes() }
        assertTrue(viaFallback.contentEquals(raw))
        val repo = LevelRepository { openLevelsGzip(app.assets) }
        assertTrue("seviye sayısı ${repo.count}", repo.count >= 10_000)
        val lines = String(raw, Charsets.UTF_8).lines().count { it.isNotBlank() }
        assertEquals(lines, repo.count)

        // 2) son seviyeden başla: ipucuyla bitir, AllDone, yeniden aç
        val container = AppContainer(app)
        val last = repo.count - 1
        runBlocking(Dispatchers.Default) {
            container.progressRepository.save(Progress(last, 5000, emptySet(), emptySet(), emptySet(), 0))
            val vm = container.gameViewModelFactory().create(GameViewModel::class.java)
            val events = ArrayList<GameEvent>()
            val job = CoroutineScope(Dispatchers.Default).launch { vm.events.collect { synchronized(events) { events += it } } }
            val s = withTimeout(60_000) { vm.state.first { it !is GameUiState.Loading } }
            assertTrue("durum: $s", s is GameUiState.Ready)
            assertEquals(repo.count, (s as GameUiState.Ready).levelNumber)
            val words = repo.get(last)!!.words.size
            repeat(words) { vm.onHintWord() }
            val done = vm.state.value as GameUiState.Ready
            assertTrue(done.levelComplete)
            // +10 seviye ödülü, +25 son ülke (Dünya turu) bonusu
            assertEquals(5000 - 30 * words + 10 + 25, done.coins)
            vm.onNextLevel()
            assertEquals(GameUiState.AllDone, vm.state.value)
            withTimeout(30_000) { container.progressRepository.progress.first { it.levelIndex == repo.count } }
            val saved = container.progressRepository.progress.first()
            assertEquals(5000 - 30 * words + 10 + 25, saved.coins)
            val vm2 = container.gameViewModelFactory().create(GameViewModel::class.java)
            withTimeout(60_000) { vm2.state.first { it !is GameUiState.Loading } }
            assertEquals(GameUiState.AllDone, vm2.state.value)
            job.cancel()
        }
    }
}
