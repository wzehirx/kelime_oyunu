package com.kelimeoyunu.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.WordDictionary
import com.kelimeoyunu.ui.game.AllDoneContent
import com.kelimeoyunu.ui.game.FakeStates
import com.kelimeoyunu.ui.game.GameContent
import com.kelimeoyunu.ui.game.GameScreen
import com.kelimeoyunu.ui.game.GameUiState
import com.kelimeoyunu.ui.game.GameViewModel
import com.kelimeoyunu.ui.game.LetterTile
import com.kelimeoyunu.ui.game.RecordingCallbacks
import com.kelimeoyunu.ui.menu.HowToPlayScreen
import com.kelimeoyunu.ui.menu.MainMenuScreen
import com.kelimeoyunu.ui.settings.SettingsScreen
import com.kelimeoyunu.ui.theme.KelimeTheme
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tüm ekranlar küçük ekranda (320dp) ve 1.0 / 2.0 yazı ölçeğinde taranır: etiketsiz tıklanabilir yok,
 * hedefler >= 48dp, yatay taşma yok, tam bir başlangıç odağı, en az bir başlık.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w320dp-h568dp")
class ScreensA11yAdversarialTest {
    @get:Rule val rule = createComposeRule()
    private val rec = RecordingCallbacks()
    private var fontScale by mutableStateOf(1f)
    private var density = 1f
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dir = kotlin.io.path.createTempDirectory("scr").toFile()

    @After fun tearDown() { scope.cancel(); dir.deleteRecursively() }

    private fun host(content: @androidx.compose.runtime.Composable () -> Unit) = rule.setContent {
        val d = LocalDensity.current
        density = d.density
        CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) { KelimeTheme { content() } }
    }

    private fun label(n: SemanticsNode): String {
        val cd = n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString("")
        val tx = n.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString("") { it.text }
        return cd.ifBlank { tx }
    }

    private fun scan(name: String, expectInitialFocus: Boolean = true, expectHeading: Boolean = true, checkBounds: Boolean = true) {
        for (scale in listOf(1f, 2f)) {
            fontScale = scale
            rule.waitForIdle()
            val ctx = "$name @fontScale=$scale"
            val clickables = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            assertTrue("$ctx: tıklanabilir öğe bulunamadı (tarama boş geçmesin)", clickables.isNotEmpty())
            clickables.forEach { n ->
                assertTrue("$ctx: etiketsiz tıklanabilir ${n.config}", label(n).isNotBlank())
                val wDp = n.size.width / density; val hDp = n.size.height / density
                assertTrue("$ctx: '${label(n)}' hedefi ${wDp}x${hDp}dp < 48dp", wDp >= 48f && hDp >= 48f)
            }
            if (checkBounds) {
                val rootW = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
                rule.onAllNodes(androidx.compose.ui.test.isRoot().not()).fetchSemanticsNodes().forEach { n ->
                    val r: Rect = n.boundsInRoot
                    assertTrue("$ctx: '${label(n)}' yatayda taşıyor $r (genişlik $rootW)", r.left >= -1f && r.right <= rootW + 1f)
                }
            }
            if (expectHeading) assertTrue("$ctx: başlık yok", rule.onAllNodes(isHeading()).fetchSemanticsNodes().isNotEmpty())
            if (expectInitialFocus) {
                val focused = rule.onAllNodes(isFocused()).fetchSemanticsNodes()
                assertEquals("$ctx: tam bir odaklı düğüm olmalı", 1, focused.size)
            }
        }
    }

    @Test fun mainMenu() {
        host { MainMenuScreen(1, {}, {}, {}) }
        scan("menü")
    }

    @Test fun mainMenuWhileProgressStillLoading() {
        host { MainMenuScreen(null, {}, {}, {}) }
        scan("menü(yükleniyor)")
    }

    @Test fun howToPlay() {
        host { HowToPlayScreen({}) }
        scan("nasıl oynanır")
    }

    @Test fun settings() {
        host { SettingsScreen(true, {}, {}, {}) }
        scan("ayarlar")
    }

    @Test fun settingsResetDialog() {
        host { SettingsScreen(true, {}, {}, {}) }
        rule.onNodeWithContentDescription("İlerlemeyi sıfırla").performClick()
        // Diyalog ayrı pencere: sınırlar kök penceresine göre değil; etiket ve hedef boyutu taranır.
        scan("sıfırlama diyaloğu", expectInitialFocus = false, expectHeading = false, checkBounds = false)
        val labels = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { label(it) }
        assertTrue(labels.toString(), labels.containsAll(listOf("Evet, sıfırla", "Vazgeç")))
    }

    private fun bigPad(): GameUiState.Ready {
        val base = FakeStates.ready(currentWord = "ça", usedIndices = setOf(0, 3))
        // 10 harf: 3 satır; Türkçe harfler dahil.
        return base.copy(tiles = "çiçeklerin".mapIndexed { i, c -> LetterTile(i, c, i in setOf(0, 3)) })
    }

    @Test fun gameScreen() {
        host { GameContent(bigPad(), rec.callbacks) }
        scan("oyun")
    }

    @Test fun levelComplete() {
        host { GameContent(FakeStates.ready(complete = true), rec.callbacks) }
        scan("seviye sonu")
    }

    @Test fun allDone() {
        host { AllDoneContent({}) }
        scan("AllDone")
    }

    @Test fun errorScreenFromRealViewModel() {
        val repo = ProgressRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "p.preferences_pb") })
        val vm = GameViewModel(LevelRepository { ByteArrayInputStream(ByteArray(0)) }, WordDictionary(setOf("a")), repo, Random(1), Dispatchers.Default)
        host { GameScreen(vm, {}, {}) }
        rule.waitUntil(30_000) { rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        scan("hata ekranı")
    }

    @Test fun duplicateLabelsAmongClickablesAreOnlyRepeatedLetters() {
        host { GameContent(bigPad(), rec.callbacks) }
        val labels = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { label(it) }
        val dups = labels.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("harf dışı yinelenen etiket: $dups", dups.all { it.startsWith("Harf ") })
    }
}
