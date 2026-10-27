package com.kelimeoyunu

import com.kelimeoyunu.domain.GameEconomy
import android.app.Application
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isFocused
import androidx.test.core.app.ApplicationProvider
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.ui.AppRoot
import com.kelimeoyunu.ui.theme.KelimeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Gerçek AppContainer (gerçek words.txt ve 10.000 seviye) ile menüden oyuna, ayarlara ve geriye tam akış. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class AppRootAdversarialTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var container: AppContainer
    private lateinit var a11yManager: AccessibilityManager

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        container = AppContainer(app)
        a11yManager = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        Shadows.shadowOf(a11yManager).setEnabled(true)
    }

    private fun show() = rule.setContent { KelimeTheme { AppRoot(container) } }
    private fun exists(cd: String) = rule.onAllNodes(hasContentDescription(cd)).fetchSemanticsNodes().isNotEmpty()
    private fun existsPrefix(prefix: String) = rule.onAllNodes(
        androidx.compose.ui.test.SemanticsMatcher("cd startsWith $prefix") {
            it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { s -> s.startsWith(prefix) }
        },
    ).fetchSemanticsNodes().isNotEmpty()
    private fun tap(cd: String) = rule.onNodeWithContentDescription(cd).performScrollTo().performClick()
    private fun announcements(): List<String> = Shadows.shadowOf(a11yManager).sentAccessibilityEvents
        .filter { it.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT }
        .map { it.text.joinToString(" ") }
    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
    private fun focusedLabel(): String = rule.onAllNodes(isFocused()).fetchSemanticsNodes().joinToString(" | ") {
        it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString("") +
            it.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString("") { t -> t.text }
    }

    /** Harfe göre sırayla, henüz seçilmemiş ilk "Harf X" düğmesine dokunur. */
    private fun typeWord(word: String) {
        for (ch in word) {
            val cd = "Harf ${com.kelimeoyunu.domain.TurkishText.upper(ch.toString())}"
            val nodes = rule.onAllNodes(hasContentDescription(cd))
            nodes[0].performScrollTo().performClick()
        }
    }

    @Test fun fullJourneyMenuGameLevelEndNextLevelBackSettingsReset() {
        show()
        rule.waitUntil(60_000) { existsPrefix("Devam et, Türkiye, 1 / 20") }
        rule.onNodeWithContentDescription("Devam et, Türkiye, 1 / 20").assertIsFocused()

        tap("Devam et, Türkiye, 1 / 20")
        rule.waitUntil(60_000) { exists("Seviye 1, Türkiye 1 / 20. 50 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5") }
        rule.onNodeWithContentDescription("Seviye 1, Türkiye 1 / 20. 50 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5").assertIsFocused()

        typeWord("yok"); tap("Gönder")
        rule.waitUntil(30_000) { exists("Kelime 1, 3 harfli, bulundu: YOK") }
        typeWord("koy"); tap("Gönder")
        rule.waitUntil(30_000) { exists("Sonraki seviye") }
        rule.onNodeWithContentDescription("Sonraki seviye").assertIsFocused()
        rule.waitUntil(10_000) { announcements().any { it.contains("Seviye 1 tamamlandı") } }
        val finale = announcements().last { it.contains("tamamlandı") }
        // Test dokunuşları ms aralıklı; son harf ("Ş") aynı 120 ms penceresine girebilir, bu yüzden sonek denetlenir.
        assertTrue(finale, finale.endsWith("Doğru, KOY. ${GameEconomy.LEVEL_REWARD} jeton kazandın. Seviye 1 tamamlandı"))

        tap("Sonraki seviye")
        rule.waitUntil(30_000) { exists("Seviye 2, Türkiye 2 / 20. ${50 + GameEconomy.LEVEL_REWARD} jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5") }
        rule.onNodeWithContentDescription("Seviye 2, Türkiye 2 / 20. ${50 + GameEconomy.LEVEL_REWARD} jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5").assertIsFocused()

        back() // oyundan menüye
        rule.waitUntil(30_000) { exists("Devam et, Türkiye, 2 / 20") }
        rule.onNodeWithContentDescription("Devam et, Türkiye, 2 / 20").assertIsFocused()

        tap("Ayarlar")
        rule.waitUntil(30_000) { rule.onAllNodes(androidx.compose.ui.test.isHeading()).fetchSemanticsNodes().isNotEmpty() && exists("Ses efektleri") }
        assertEquals("Ayarlar", focusedLabel())
        tap("İlerlemeyi sıfırla")
        rule.onNodeWithText("İlerleme sıfırlansın mı?").assertExists()
        rule.onNodeWithText("Evet, sıfırla").performClick()
        rule.waitUntil(10_000) { announcements().any { it == "İlerleme sıfırlandı" } }
        tap("Geri")
        rule.waitUntil(30_000) { exists("Devam et, Türkiye, 1 / 20") }
        back() // menüde geri: uygulama davranışı BackHandler'sız (etkinlik kapanır), çökmemeli
    }

    @Test fun settingsBackReturnsToMenuAndHowToPlayBackReturnsToMenu() {
        show()
        rule.waitUntil(60_000) { existsPrefix("Devam et") }
        tap("Nasıl oynanır"); rule.waitUntil(10_000) { exists("Geri") }
        back(); rule.waitUntil(10_000) { exists("Ayarlar") }
        tap("Ayarlar"); rule.waitUntil(10_000) { exists("Geri") }
        back(); rule.waitUntil(10_000) { exists("Nasıl oynanır") }
    }

    @Test fun menuDoesNotOfferNonexistentLevel10001AfterAllLevelsAreDone() {
        val count = container.levelRepository.count
        assertEquals(10_000, count)
        runBlocking { container.progressRepository.save(Progress(levelIndex = count)) }
        show()
        rule.waitUntil(60_000) { existsPrefix("Devam et") }
        Thread.sleep(500)
        rule.waitForIdle()
        val labels = rule.onAllNodes(androidx.compose.ui.test.hasClickAction()).fetchSemanticsNodes()
            .map { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString("") }
        assertFalse("var olmayan seviye sunuluyor: $labels", labels.any { it.contains("10001") })
    }

    @Test fun allDoneEnteredFromMenuGivesContextNotJustAButton() {
        val count = container.levelRepository.count
        runBlocking { container.progressRepository.save(Progress(levelIndex = count)) }
        show()
        rule.waitUntil(60_000) { existsPrefix("Devam et") }
        rule.onAllNodes(androidx.compose.ui.test.hasClickAction())[0].performClick()
        rule.waitUntil(60_000) { exists("Ana menüye dön") }
        rule.waitForIdle()
        Thread.sleep(500)
        val focused = focusedLabel()
        val spokenAnything = announcements().any { it.contains("Tebrikler") || it.contains("tamamla") }
        assertTrue(
            "TalkBack kullanıcısı yalnızca '$focused' duyar; 'Tüm seviyeleri tamamladın' bağlamı ne odakta ne duyuruda",
            focused.contains("tamamla") || focused.contains("Tebrik") || spokenAnything,
        )
    }

    @Test fun savedCompletedLevelReopenedGivesContextNotJustNextButton() {
        // Seviye 1 bitmiş ama "Sonraki seviye"ye basılmadan çıkılmış kayıt.
        runBlocking {
            container.progressRepository.save(Progress(levelIndex = 0, coins = 50 + GameEconomy.LEVEL_REWARD, found = setOf("yok", "koy")))
        }
        show()
        rule.waitUntil(60_000) { existsPrefix("Devam et") }
        rule.onAllNodes(androidx.compose.ui.test.hasClickAction())[0].performClick()
        rule.waitUntil(60_000) { exists("Sonraki seviye") }
        rule.waitForIdle()
        Thread.sleep(500)
        val focused = focusedLabel()
        val spokenAnything = announcements().any { it.contains("Seviye 1") }
        assertTrue(
            "odak '$focused' ve hiçbir duyuru yok: kullanıcı hangi seviyenin bittiğini bilmez",
            focused.contains("Seviye 1") || spokenAnything,
        )
    }
}
