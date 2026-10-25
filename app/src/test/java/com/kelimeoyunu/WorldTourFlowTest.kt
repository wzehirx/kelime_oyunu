package com.kelimeoyunu

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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.data.loadPlaces
import com.kelimeoyunu.ui.AppRoot
import com.kelimeoyunu.ui.theme.KelimeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Gerçek AppContainer (gerçek places.json + 10.000 seviye) ile menü, Dünya turu ekranı ve ülke girişi duyurusu. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class WorldTourFlowTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var container: AppContainer
    private lateinit var a11y: AccessibilityManager

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        container = AppContainer(app)
        a11y = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        Shadows.shadowOf(a11y).setEnabled(true)
    }

    private fun show() = rule.setContent { KelimeTheme { AppRoot(container) } }
    private fun exists(cd: String) = rule.onAllNodes(hasContentDescription(cd)).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(cd: String, ms: Long = 60_000) = try {
        rule.waitUntil(ms) { exists(cd) }
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
        throw AssertionError("'$cd' bulunamadı:\n" + rule.onRoot(useUnmergedTree = false).printToStringSafe(), e)
    }
    private fun androidx.compose.ui.test.SemanticsNodeInteraction.printToStringSafe() =
        this.printToString()
    private fun tap(cd: String) = rule.onNodeWithContentDescription(cd).performScrollTo().performClick()
    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
    private fun announcements() = Shadows.shadowOf(a11y).sentAccessibilityEvents
        .filter { it.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT }.map { it.text.joinToString(" ") }

    @Test fun containerBuildsTheFullTourFromRealAssets() {
        val tour = container.worldTour
        assertNotNull(tour)
        assertEquals(100, tour!!.placeCount)
        assertEquals(container.levelRepository.count, tour.totalLevels)
        assertEquals(10_000, tour.totalLevels)
    }

    @Test fun menuToWorldTourAndBackRestoresFocus() {
        show()
        waitFor("Devam et, Türkiye, 1 / 20")
        rule.onNodeWithContentDescription("Devam et, Türkiye, 1 / 20").assertIsFocused()
        tap("Dünya turu")
        waitFor("Türkiye, 0 / 20 seviye, şu an buradasın")
        rule.onNodeWithContentDescription("Türkiye, 0 / 20 seviye, şu an buradasın").assertIsFocused()
        waitFor(loadPlaces { TestAssets.open("places.json") }[1].ad + ", kilitli")
        back()
        waitFor("Dünya turu")
        rule.waitUntil(10_000) { runCatching { rule.onNodeWithContentDescription("Dünya turu").assertIsFocused() }.isSuccess }
    }

    @Test fun savedProgressMarksCompletedPlacesAndFocusesCurrent() {
        val places = loadPlaces { TestAssets.open("places.json") }
        runBlocking { container.progressRepository.save(Progress(levelIndex = 205, coins = 50)) }
        show()
        waitFor("Devam et, ${places[10].ad}, 6 / 50")
        tap("Dünya turu")
        waitFor("${places[10].ad}, 5 / 50 seviye, şu an buradasın")
        rule.onNodeWithContentDescription("${places[10].ad}, 5 / 50 seviye, şu an buradasın").assertIsFocused()
        rule.onNode(androidx.compose.ui.test.hasText("Dünya turu, 10 / 100 ülke tamamlandı")).assertExists()
    }

    @Test fun openingFreshGameAnnouncesTurkeyExactlyOnce() {
        show()
        waitFor("Devam et, Türkiye, 1 / 20")
        tap("Devam et, Türkiye, 1 / 20")
        waitFor("Seviye 1, Türkiye 1 / 20. 50 jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5")
        rule.waitUntil(10_000) { announcements().any { it.startsWith("Yeni ülke: Türkiye, Asya. Başkenti Ankara. 20 seviyelik yolculuk başlıyor. Biliyor muydun?") } }
        Thread.sleep(300)
        rule.waitForIdle()
        assertEquals(1, announcements().count { it.contains("Yeni ülke") })
    }
}
