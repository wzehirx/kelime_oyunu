package com.kelimeoyunu.ui.game

import com.kelimeoyunu.domain.GameEconomy
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kelimeoyunu.a11y.AnnouncingFeedbackSink
import com.kelimeoyunu.a11y.Announcer
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.audio.SfxPlayer
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.WordDictionary
import com.kelimeoyunu.ui.theme.KelimeTheme
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gerçek ViewModel + gerçek GameScreen + gerçek Announcer/AnnouncingFeedbackSink; yalnızca konuşma
 * (view.announceForAccessibility) ve ses çıkışı kaydedilir. Seviye 1: TEN/NET, seviye 2: MET/TEM.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class GameFlowAdversarialTest {
    @get:Rule val rule = createComposeRule()
    private val dir = kotlin.io.path.createTempDirectory("flowadv").toFile()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val spoken = CopyOnWriteArrayList<String>()
    private val sounds = CopyOnWriteArrayList<Sfx>()
    private lateinit var vm: GameViewModel

    @After fun tearDown() { scope.cancel(); dir.deleteRecursively() }

    private fun gz(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }

    private fun start(coins: Int = 50) {
        val repo = ProgressRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "p.preferences_pb") })
        val levels = LevelRepository {
            ByteArrayInputStream(gz("1|ten|ten,0,0,y;net,0,2,d\n2|etm|met,0,0,y;tem,0,2,d\n"))
        }
        vm = GameViewModel(levels, WordDictionary(setOf("ten", "net", "et", "te")), repo, Random(1), Dispatchers.Default)
        val announcer = Announcer({ spoken += it }, scope, batchMs = 40)
        val feedback = CompositeFeedbackSink(
            AnnouncingFeedbackSink(announcer, { vm.state.value }, { System.nanoTime() / 1_000_000 }),
            SoundFeedbackSink(SfxPlayer { sfx, _ -> sounds += sfx }),
        )
        rule.setContent { KelimeTheme { GameScreen(vm, feedback, onExitToMenu = {}) } }
        rule.waitUntil(30_000) { exists("Harf T") }
    }

    private fun exists(cd: String) = rule.onAllNodes(hasContentDescription(cd)).fetchSemanticsNodes().isNotEmpty()
    private fun tap(cd: String) = rule.onNodeWithContentDescription(cd).performScrollTo().performClick()
    private fun waitSpoken(n: Int) = rule.waitUntil(10_000) { spoken.size >= n }

    @Test fun announcementsComeInTheRightOrderAndLevelEndIsOneBatch() {
        start()
        tap("Harf T"); waitSpoken(1)
        tap("Harf E"); waitSpoken(2)
        tap("Harf N"); waitSpoken(3)
        tap("Gönder"); waitSpoken(4)
        tap("Harf N"); waitSpoken(5)
        tap("Harf E"); waitSpoken(6)
        tap("Harf T"); waitSpoken(7)
        tap("Gönder"); waitSpoken(8)
        assertEquals(
            listOf("T", "E", "N", "Doğru, TEN", "N", "E", "T", "Doğru, NET. ${GameEconomy.LEVEL_REWARD} jeton kazandın. Seviye 1 tamamlandı"),
            spoken.toList(),
        )
        rule.waitUntil(10_000) { exists("Sonraki seviye") }
        rule.onNodeWithContentDescription("Sonraki seviye").assertIsFocused()
        // Ses: harfler, doğru, doğru + jeton + seviye bitti
        assertEquals(
            listOf(Sfx.LETTER_1, Sfx.LETTER_2, Sfx.LETTER_3, Sfx.CORRECT, Sfx.LETTER_1, Sfx.LETTER_2, Sfx.LETTER_3, Sfx.CORRECT, Sfx.COIN, Sfx.LEVEL_COMPLETE),
            sounds.toList(),
        )
    }

    @Test fun nextLevelMovesFocusToNewLevelHeader() {
        start()
        tap("Harf T"); tap("Harf E"); tap("Harf N"); tap("Gönder")
        rule.waitUntil(10_000) { exists("Kelime 1, 3 harfli, bulundu: TEN") }
        tap("Harf N"); tap("Harf E"); tap("Harf T"); tap("Gönder")
        rule.waitUntil(10_000) { exists("Sonraki seviye") }
        tap("Sonraki seviye")
        rule.waitUntil(10_000) { exists("Seviye 2. ${50 + GameEconomy.LEVEL_REWARD} jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5") }
        rule.onNodeWithContentDescription("Seviye 2. ${50 + GameEconomy.LEVEL_REWARD} jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5").assertIsFocused()
        assertFalse("eski seviye düğmesi kalmamalı", exists("Sonraki seviye"))
    }

    @Test fun tenRapidTapsKeepStateConsistentAndOnlyLastLetterOfABurstIsSpoken() {
        start()
        val seq = listOf("T", "T", "E", "E", "N", "N", "T", "E", "N", "T")
        // Her harfe en çok bir kez basılabilir; "seçildi" etiketli düğmeye basmak etkisiz olmalı.
        val labels = mutableSetOf<String>()
        seq.forEach { l ->
            val cd = if (l in labels) "Harf $l, seçildi" else "Harf $l"
            labels += l
            tap(cd)
        }
        rule.waitUntil(10_000) { exists("Oluşturulan kelime: TEN") }
        rule.waitUntil(10_000) { spoken.isNotEmpty() }
        Thread.sleep(300)
        val tokens = spoken.flatMap { it.split(". ") }
        assertTrue("sadece T/E/N okunabilir: $tokens", tokens.all { it in setOf("T", "E", "N") })
        assertEquals("N", tokens.last())
        assertTrue("üçten fazla harf duyurusu: $tokens", tokens.size <= 3)
        assertEquals(listOf(Sfx.LETTER_1, Sfx.LETTER_2, Sfx.LETTER_3), sounds.toList())
    }

    @Test fun doubleSubmitAfterCorrectDoesNotPlayErrorBuzzWhileSpeechIsSuppressed() {
        start()
        tap("Harf T"); tap("Harf E"); tap("Harf N")
        tap("Gönder"); tap("Gönder") // çift gönderim
        rule.waitUntil(10_000) { spoken.isNotEmpty() }
        Thread.sleep(300)
        assertFalse("konuşma bastırıldı ama hata sesi çaldı: $sounds", sounds.contains(Sfx.ERROR))
        assertFalse(spoken.any { it.contains("Önce harf seç") })
    }

    @Test fun emptySubmitOnItsOwnIsSpokenAndHasErrorSound() {
        start()
        tap("Gönder")
        rule.waitUntil(10_000) { spoken.isNotEmpty() }
        assertEquals(listOf("Önce harf seç"), spoken.toList())
        assertEquals(listOf(Sfx.ERROR), sounds.toList())
    }

    @Test fun reTappingSelectedLetterDoesNotChangeWordOrDoubleCount() {
        start()
        tap("Harf T")
        rule.waitUntil(10_000) { exists("Harf T, seçildi") }
        repeat(5) { tap("Harf T, seçildi") }
        rule.onNodeWithContentDescription("Oluşturulan kelime: T").assertExists()
        Thread.sleep(200)
        assertEquals(1, sounds.count { it == Sfx.LETTER_1 })
    }
}
