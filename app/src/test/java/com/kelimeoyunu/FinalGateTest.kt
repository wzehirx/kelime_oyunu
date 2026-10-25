package com.kelimeoyunu

import android.app.Application
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.kelimeoyunu.a11y.Announcer
import com.kelimeoyunu.a11y.AnnouncingFeedbackSink
import com.kelimeoyunu.audio.FakeSoundBackend
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.audio.SoundManager
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.ui.AppRoot
import com.kelimeoyunu.ui.game.CompositeFeedbackSink
import com.kelimeoyunu.ui.game.GameScreen
import com.kelimeoyunu.ui.game.GameViewModel
import com.kelimeoyunu.ui.game.SoundFeedbackSink
import com.kelimeoyunu.ui.theme.KelimeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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

/**
 * Son kapı: gerçek AppContainer (gerçek words.txt + 10.000 seviye + gerçek DataStore dosyası) ile bir TalkBack
 * kullanıcısının gerçek 1. seviyeyi (harfler "uhş": şuh, huş) yalnızca etiketli düğmelerle bitirmesi,
 * hata yolları, kayıt/geri yükleme ve ses sessizliği. Erişilebilirlik duyuruları AccessibilityManager'a
 * gönderilen TYPE_ANNOUNCEMENT olaylarından okunur.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class FinalGateTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var container: AppContainer
    private lateinit var a11y: AccessibilityManager
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        container = AppContainer(app)
        a11y = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        Shadows.shadowOf(a11y).setEnabled(true)
    }

    @After fun tearDown() { scope.cancel() }

    // ---------- yardımcılar ----------

    private fun show() = rule.setContent { KelimeTheme { AppRoot(container) } }
    private fun exists(cd: String) = rule.onAllNodes(hasContentDescription(cd)).fetchSemanticsNodes().isNotEmpty()
    private fun existsPrefix(prefix: String) = rule.onAllNodes(
        SemanticsMatcher("cd startsWith $prefix") {
            it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { s -> s.startsWith(prefix) }
        },
    ).fetchSemanticsNodes().isNotEmpty()
    private fun tap(cd: String) = rule.onNodeWithContentDescription(cd).performScrollTo().performClick()
    private fun waitFor(cd: String, ms: Long = 60_000) = rule.waitUntil(ms) { exists(cd) }
    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
    private fun announcements(): List<String> = Shadows.shadowOf(a11y).sentAccessibilityEvents
        .filter { it.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT }
        .map { it.text.joinToString(" ") }
    private fun heard(fragment: String) = announcements().any { it.contains(fragment) }
    private fun waitHeard(fragment: String, ms: Long = 10_000) =
        rule.waitUntil(ms) { heard(fragment) }
    private fun letter(ch: Char) = "Harf ${TurkishText.upper(ch.toString())}"
    private fun typeWord(word: String) = word.forEach { tap(letter(it)) }
    private fun progress(): Progress = runBlocking { container.progressRepository.progress.first() }

    // ---------- 1. Tam yolculuk ----------

    @Test fun realPlayerFinishesLevelOneWithErrorsAndHintThenResumesAtLevelTwoAfterRestore() {
        val tester = StateRestorationTester(rule)
        tester.setContent { KelimeTheme { AppRoot(container) } }
        rule.waitUntil(60_000) { exists("Devam et, Seviye 1") }
        rule.onNodeWithContentDescription("Devam et, Seviye 1").assertIsFocused()
        tap("Devam et, Seviye 1")
        val header = "Seviye 1. 50 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5"
        waitFor(header)
        rule.onNodeWithContentDescription(header).assertIsFocused()

        // a) Henüz harf seçmeden tek başına Gönder: bastırılmamalı, "Önce harf seç" duyurulmalı.
        tap("Gönder")
        waitHeard("Önce harf seç")

        // b) Geçersiz kelime; kelime silinmez, Temizle ile boşaltılır.
        typeWord("uhş"); tap("Gönder")
        waitHeard("Geçersiz kelime")
        rule.onNodeWithContentDescription("Oluşturulan kelime: UHŞ").assertExists()
        tap("Temizle")
        waitHeard("Temizlendi")
        rule.onNodeWithContentDescription("Oluşturulan kelime: boş").assertExists()

        // c) Bonus kelime (sözlükte var, seviyede yok): sayaç 1/5.
        typeWord("hu"); tap("Gönder")
        waitHeard("Bonus kelime, HU")
        waitFor("Seviye 1. 50 jeton. Kalan kelime: 2. Bonus sayacı: 1 / 5")

        // d) İpucu: bakiye düşer, duyuru DÜŞÜLMÜŞ bakiyeyi söyler; ekran başlığı aynı sayıyı verir.
        tap("İpucu, harf aç, 10 jeton")
        waitHeard("İpucu kullanıldı, kalan jeton: 40")
        waitFor("Seviye 1. 40 jeton. Kalan kelime: 2. Bonus sayacı: 1 / 5")

        // e) İlk kelime, sonra aynı kelime ikinci kez.
        typeWord("şuh"); tap("Gönder")
        waitHeard("Doğru, ŞUH")
        waitFor("Seviye 1. 40 jeton. Kalan kelime: 1. Bonus sayacı: 1 / 5")
        typeWord("şuh"); tap("Gönder")
        waitHeard("Bu kelime zaten bulundu")
        waitFor("Seviye 1. 40 jeton. Kalan kelime: 1. Bonus sayacı: 1 / 5") // değişmedi

        // f) Son kelime: seviye biter, odak "Sonraki seviye"de, duyuru tek cümlede.
        typeWord("huş"); tap("Gönder")
        waitFor("Sonraki seviye", 30_000)
        rule.onNodeWithContentDescription("Sonraki seviye").assertIsFocused()
        waitHeard("Seviye 1 tamamlandı")
        val finale = announcements().last { it.contains("tamamlandı") }
        assertTrue(finale, finale.endsWith("Doğru, HUŞ. 10 jeton kazandın. Seviye 1 tamamlandı"))
        rule.onNodeWithText("Tüm kelimeleri buldun. Toplam jeton: 50. Bonus sayacı: 1 / 5.").assertExists()

        // g) Sonraki seviye: 2. seviye başlar, odak başlıkta.
        tap("Sonraki seviye")
        val header2 = "Seviye 2. 50 jeton. Kalan kelime: 2. Bonus sayacı: 1 / 5"
        waitFor(header2)
        rule.onNodeWithContentDescription(header2).assertIsFocused()

        // h) İlerleme diske yazıldı (yeni ViewModel/yeniden açılış bunu okur).
        rule.waitUntil(15_000) { progress().levelIndex == 1 }
        val p = progress()
        assertEquals(1, p.levelIndex); assertEquals(50, p.coins); assertEquals(1, p.bonusMeter)
        assertTrue("2. seviyede bulunmuş kelime olmamalı: ${p.found}", p.found.isEmpty())

        // i) Süreç ölümü/yeniden açılış benzetimi: saveable durum geri yüklenir, yeni ViewModel kayıttan 2. seviyeyi açar.
        tester.emulateSavedInstanceStateRestore()
        waitFor(header2)
        rule.onNodeWithContentDescription(header2).assertIsFocused()

        // j) Oyundan geri tuşu: menü artık "Seviye 2" der, odak Devam et'te.
        back()
        rule.waitUntil(30_000) { exists("Devam et, Seviye 2") }
        rule.onNodeWithContentDescription("Devam et, Seviye 2").assertIsFocused()
    }

    // ---------- 2. Yetersiz jeton ----------

    @Test fun notEnoughCoinsIsAnnouncedAndNothingChanges() {
        runBlocking { container.progressRepository.save(Progress(coins = 5)) }
        show()
        rule.waitUntil(60_000) { exists("Devam et, Seviye 1") }
        tap("Devam et, Seviye 1")
        val header = "Seviye 1. 5 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5"
        waitFor(header)
        tap("İpucu, harf aç, 10 jeton")
        waitHeard("Yeterli jeton yok")
        tap("Kelime aç, 30 jeton")
        rule.waitUntil(10_000) { announcements().count { it.contains("Yeterli jeton yok") } >= 2 }
        assertFalse(heard("İpucu kullanıldı"))
        waitFor(header)
    }

    // ---------- 3. Kelime aç ile bitirme: duyurulan bakiye seviye ödülünü içermez ----------

    @Test fun hintWordThenFinishLevelAnnouncesBalanceAfterCostOnly() {
        show()
        rule.waitUntil(60_000) { exists("Devam et, Seviye 1") }
        tap("Devam et, Seviye 1")
        waitFor("Seviye 1. 50 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5")
        tap("Kelime aç, 30 jeton")
        waitHeard("İpucu kullanıldı, kalan jeton: 20")
        waitFor("Seviye 1. 20 jeton. Kalan kelime: 1. Bonus sayacı: 0 / 5")
        // Hangi kelimenin açıldığı rastgele: ikisini de dene; açılmış olana "zaten bulundu" denir.
        val candidates = listOf("şuh", "huş")
        for (w in candidates) {
            if (exists("Sonraki seviye")) break
            typeWord(w); tap("Gönder")
            rule.waitForIdle()
            Thread.sleep(300)
            rule.waitForIdle()
            if (!exists("Sonraki seviye")) tap("Temizle")
        }
        waitFor("Sonraki seviye", 30_000)
        rule.onNodeWithText("Tüm kelimeleri buldun. Toplam jeton: 30. Bonus sayacı: 0 / 5.").assertExists()
    }

    // ---------- 4. Ayarlar'dan ve oyundan dönüşte odak ----------

    @Test fun focusAfterSettingsAndAfterGameBackKey() {
        show()
        rule.waitUntil(60_000) { exists("Devam et, Seviye 1") }
        tap("Ayarlar"); waitFor("Ses efektleri")
        tap("Geri"); waitFor("Devam et, Seviye 1")
        rule.onNodeWithContentDescription("Ayarlar").assertIsFocused()
        tap("Nasıl oynanır"); waitFor("Geri")
        back(); waitFor("Devam et, Seviye 1")
        rule.onNodeWithContentDescription("Devam et, Seviye 1").assertIsFocused()
        tap("Devam et, Seviye 1"); waitFor("Seviye 1. 50 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5")
        back(); waitFor("Devam et, Seviye 1")
        rule.onNodeWithContentDescription("Devam et, Seviye 1").assertIsFocused()
    }

    // ---------- 5. Ayarlar ekranı süreç ölümü: ekran ve açık sıfırlama diyaloğu geri gelir ----------

    @Test fun settingsScreenAndResetDialogSurviveStateRestoration() {
        val tester = StateRestorationTester(rule)
        tester.setContent { KelimeTheme { AppRoot(container) } }
        rule.waitUntil(60_000) { exists("Devam et, Seviye 1") }
        tap("Ayarlar"); waitFor("İlerlemeyi sıfırla")
        tap("İlerlemeyi sıfırla")
        rule.onNodeWithText("İlerleme sıfırlansın mı?").assertExists()
        tester.emulateSavedInstanceStateRestore()
        waitFor("İlerlemeyi sıfırla")
        rule.onNodeWithText("İlerleme sıfırlansın mı?").assertExists()
        rule.onNodeWithText("Vazgeç").performClick()
        tap("Geri"); waitFor("Devam et, Seviye 1")
    }

    // ---------- 6. Sıfırlama sonrası durum ----------

    @Test fun resetAfterProgressReturnsToLevelOneWithStartCoins() {
        runBlocking { container.progressRepository.save(Progress(levelIndex = 41, coins = 7, found = setOf("x"), bonusMeter = 3)) }
        show()
        rule.waitUntil(60_000) { exists("Devam et, Seviye 42") }
        tap("Ayarlar"); waitFor("İlerlemeyi sıfırla")
        tap("İlerlemeyi sıfırla")
        rule.onNodeWithText("Evet, sıfırla").performClick()
        waitHeard("İlerleme sıfırlandı")
        tap("Geri"); waitFor("Devam et, Seviye 1")
        tap("Devam et, Seviye 1")
        waitFor("Seviye 1. 50 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5")
    }

    // ---------- 7. Ses: kapalıyken tam seviye boyunca sessizlik; açıkken boş Gönder ERROR çalar ----------

    @Test fun soundDisabledIsSilentThroughAWholeLevelAndLegitEmptySubmitIsAudibleWhenEnabled() {
        val fake = FakeSoundBackend()
        val sound = SoundManager { fake }
        sound.start()
        sound.enabled = false
        val vm = ViewModelProvider(ViewModelStore(), container.gameViewModelFactory())[GameViewModel::class.java]
        val announcer = Announcer({ }, scope)
        rule.setContent {
            KelimeTheme {
                GameScreen(
                    vm,
                    CompositeFeedbackSink(
                        AnnouncingFeedbackSink(announcer, { vm.state.value }, { System.nanoTime() / 1_000_000 }),
                        SoundFeedbackSink(sound) { 3 },
                    ),
                    onExitToMenu = {},
                )
            }
        }
        waitFor("Seviye 1. 50 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5")
        tap("Gönder")
        typeWord("uhş"); tap("Gönder"); tap("Temizle")
        typeWord("hu"); tap("Gönder")
        tap("İpucu, harf aç, 10 jeton")
        typeWord("şuh"); tap("Gönder"); typeWord("şuh"); tap("Gönder")
        typeWord("huş"); tap("Gönder")
        waitFor("Sonraki seviye", 30_000)
        tap("Sonraki seviye")
        waitFor("Seviye 2. 50 jeton. Kalan kelime: 2. Bonus sayacı: 1 / 5")
        assertTrue("ses kapalıyken çalınan: ${fake.played}", fake.played.isEmpty())

        Thread.sleep(900) // çift dokunuş tolerans penceresi (800 ms) geçsin
        sound.enabled = true
        tap("Gönder") // yeni seviyede, hiç harf seçmeden, anlamlı sonuçtan çok sonra değil ama hiç sonuç yok
        rule.waitUntil(5_000) { fake.played.any { it.first == Sfx.ERROR } }
        assertEquals(1, fake.played.count { it.first == Sfx.ERROR })
    }
}
