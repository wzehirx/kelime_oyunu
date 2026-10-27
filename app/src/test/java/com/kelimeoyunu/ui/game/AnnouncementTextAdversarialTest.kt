package com.kelimeoyunu.ui.game

import com.kelimeoyunu.domain.GameEconomy
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kelimeoyunu.a11y.AnnouncingFeedbackSink
import com.kelimeoyunu.a11y.Announcer
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.WordDictionary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Üretimdeki teslim sırasını taklit eder: ViewModel eylemi bitince (durum yayımlandıktan sonra)
 * olaylar sink'e verilir; AnnouncingFeedbackSink anlık durumu okur.
 */
class AnnouncementTextAdversarialTest {
    private val dir = kotlin.io.path.createTempDirectory("anntext").toFile()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val recorded = CopyOnWriteArrayList<GameEvent>()
    private val spoken = CopyOnWriteArrayList<String>()

    @After fun tearDown() { scope.cancel(); dir.deleteRecursively() }

    private fun gz(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }

    private fun vm(): GameViewModel {
        val repo = ProgressRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "p.preferences_pb") })
        val levels = LevelRepository { ByteArrayInputStream(gz("1|ten|ten,0,0,y;net,0,2,d\n2|etm|met,0,0,y;tem,0,2,d\n")) }
        val v = GameViewModel(levels, WordDictionary(setOf("ten", "net", "et")), repo, Random(1), Dispatchers.Default)
        scope.launch(Dispatchers.Unconfined) { v.events.collect { recorded += it } }
        runBlocking { withTimeout(30_000) { v.state.first { it is GameUiState.Ready } } }
        return v
    }

    private fun deliver(v: GameViewModel) {
        val announcer = Announcer({ spoken += it }, scope, batchMs = 20)
        val sink = AnnouncingFeedbackSink(announcer, { v.state.value }, { System.nanoTime() / 1_000_000 })
        recorded.toList().also { recorded.clear() }.forEach { sink.onEvent(it) }
        val deadline = System.currentTimeMillis() + 5_000
        while (spoken.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(10)
    }

    @Test fun hintWordThatFinishesLevelSaysCorrectRemainingCoins() {
        val v = vm()
        v.onLetter(0); v.onLetter(1); v.onLetter(2); v.onSubmit()
        recorded.clear()
        v.onHintWord() // 50 - HINT_WORD_COST kalır, sonra seviye ödülü LEVEL_REWARD
        deliver(v)
        val left = 50 - GameEconomy.HINT_WORD_COST
        assertEquals(
            "kalan jeton ödül eklenmeden önceki değer ($left) olmalı, ödül ayrıca duyurulur",
            "İpucu kullanıldı, kalan jeton: $left. ${GameEconomy.LEVEL_REWARD} jeton kazandın. Seviye 1 tamamlandı",
            spoken.first(),
        )
    }

    @Test fun deleteAnnouncesRemainingWordAfterTheDelete() {
        val v = vm()
        v.onLetter(0); v.onLetter(1); recorded.clear()
        v.onDelete()
        deliver(v)
        assertEquals("Silindi, kalan: T", spoken.first())
    }

    @Test fun shuffleAnnouncementListsEveryLetterOnce() {
        val v = vm()
        recorded.clear()
        v.onShuffle()
        deliver(v)
        val msg = spoken.first()
        assertEquals(setOf("T", "E", "N"), msg.removePrefix("Harfler karıştırıldı: ").split(", ").toSet())
    }
}
