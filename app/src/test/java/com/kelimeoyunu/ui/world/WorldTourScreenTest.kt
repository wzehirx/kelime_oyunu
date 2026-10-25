package com.kelimeoyunu.ui.world

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import com.kelimeoyunu.TestPlaces
import com.kelimeoyunu.domain.WorldTour
import com.kelimeoyunu.ui.assertNoUnlabeledClickables
import com.kelimeoyunu.ui.theme.KelimeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w320dp-h568dp")
class WorldTourScreenTest {
    @get:Rule val rule = createComposeRule()
    private val tour = WorldTour(TestPlaces.list(100), 10_000)
    private val said = ArrayList<String>()
    private val calls = ArrayList<String>()
    private var fontScale by mutableStateOf(1f)
    private var density = 1f

    // 0 tabanlı 205: ilk 10 ülke (20'şer) bitti, 11. ülkede (Ülke 10, 50 seviyelik) 5 seviye tamam.
    private fun show(current: Int = 205) = rule.setContent {
        val d = LocalDensity.current
        density = d.density
        CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
            KelimeTheme { WorldTourScreen(tour, current, { said += it }, { calls += "back" }) }
        }
    }

    private fun label(n: SemanticsNode): String =
        n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString("")

    private val currentLabel = "Ülke 10, 5 / 50 seviye, şu an buradasın"

    @Test fun titleCountsCompletedPlaces() {
        show()
        rule.onNode(isHeading()).assertExists()
        rule.onNode(isHeading() and androidx.compose.ui.test.hasText("Dünya turu, 10 / 100 ülke tamamlandı")).assertExists()
    }

    @Test fun focusStartsOnCurrentPlaceAndLabelsCoverAllThreeStates() {
        show()
        rule.onNodeWithContentDescription(currentLabel).assertHasClickAction().assertIsFocused()
        rule.onNodeWithContentDescription("Ülke 11, kilitli").assertHasClickAction()
        rule.onNode(hasScrollAction()).performScrollToIndex(0)
        rule.onNodeWithContentDescription(
            "Türkiye, tamamlandı. Başkenti Ankara. Türkiye iki kıtada yer alır. Kapadokya'da peri bacaları vardır.",
        ).assertHasClickAction()
    }

    @Test fun eachRowIsOneNodeWithNoSecondReading() {
        show()
        val row = rule.onNodeWithContentDescription(currentLabel).fetchSemanticsNode()
        assertTrue(row.config.getOrNull(SemanticsProperties.Text).isNullOrEmpty())
        // görsel metin birleşik ağaçta yok (çift okuma olmaz)
        assertEquals(0, rule.onAllNodes(androidx.compose.ui.test.hasText("Ülke 10")).fetchSemanticsNodes().size)
    }

    @Test fun lockedTapSaysNotReachedAndCurrentTapRereads() {
        show()
        rule.onNodeWithContentDescription("Ülke 11, kilitli").performClick()
        rule.onNodeWithContentDescription(currentLabel).performClick()
        assertEquals(listOf("Henüz bu ülkeye ulaşmadın", currentLabel), said)
    }

    @Test fun backButtonCallsBack() {
        show()
        rule.onNodeWithContentDescription("Geri").performClick()
        assertEquals(listOf("back"), calls)
    }

    @Test fun freshGameFocusesFirstPlaceInProgress() {
        show(0)
        rule.onNodeWithContentDescription("Türkiye, 0 / 20 seviye, şu an buradasın").assertIsFocused()
        rule.onNodeWithContentDescription("Ülke 1, kilitli").assertExists()
    }

    @Test fun allDoneShowsEverythingCompletedAndFocusesHeading() {
        show(10_000)
        rule.onNode(isHeading() and androidx.compose.ui.test.hasText("Dünya turu, 100 / 100 ülke tamamlandı")).assertIsFocused()
    }

    @Test fun fontScale2HasNoOverflowAndLabeledTallTargets() {
        show()
        for (scale in listOf(1f, 2f)) {
            fontScale = scale
            rule.waitForIdle()
            assertNoUnlabeledClickables(rule)
            val rootW = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
            rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { n ->
                assertTrue("${label(n)} @$scale genişlik", n.size.width <= rootW + 1 && n.boundsInRoot.left >= -1f && n.boundsInRoot.right <= rootW + 1)
                assertTrue("${label(n)} @$scale 48dp", n.size.height / density >= 48f)
            }
            rule.onAllNodes(isRoot().not()).fetchSemanticsNodes().forEach {
                assertTrue("@$scale taşma ${it.boundsInRoot}", it.boundsInRoot.right <= rootW + 1)
            }
        }
        rule.onNodeWithContentDescription("Geri").assertExists()
    }
}
