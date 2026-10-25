package com.kelimeoyunu.ui.world

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.kelimeoyunu.TestPlaces
import com.kelimeoyunu.domain.PlacePosition
import com.kelimeoyunu.ui.assertNoUnlabeledClickables
import com.kelimeoyunu.ui.game.FakeStates
import com.kelimeoyunu.ui.game.GameContent
import com.kelimeoyunu.ui.game.RecordingCallbacks
import com.kelimeoyunu.ui.menu.HowToPlayParagraphs
import com.kelimeoyunu.ui.menu.MainMenuScreen
import com.kelimeoyunu.ui.theme.KelimeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class WorldTourMenuAndHeaderTest {
    @get:Rule val rule = createComposeRule()
    private val calls = ArrayList<String>()
    private val pos = PlacePosition(TestPlaces.turkey, 0, 1, 20)

    private fun menu(focusWorld: Boolean = false, withTour: Boolean = true) = rule.setContent {
        KelimeTheme {
            MainMenuScreen(
                1, { calls += "continue" }, { calls += "howto" }, { calls += "settings" },
                place = pos, onWorldTour = if (withTour) ({ calls += "world" }) else null, focusWorld = focusWorld,
            )
        }
    }

    private fun clickLabels() = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        .map { it.config.getOrNull(SemanticsProperties.ContentDescription)?.first() }

    @Test fun menuHasWorldButtonAndPlaceAwareContinue() {
        menu()
        assertEquals(listOf("Devam et, Türkiye, 1 / 20", "Dünya turu", "Nasıl oynanır", "Ayarlar"), clickLabels())
        rule.onNodeWithContentDescription("Devam et, Türkiye, 1 / 20").assertIsFocused()
        rule.onNodeWithContentDescription("Dünya turu").performClick()
        assertEquals(listOf("world"), calls)
        assertNoUnlabeledClickables(rule)
    }

    @Test fun returningFromWorldTourFocusesItsButton() {
        menu(focusWorld = true)
        rule.onNodeWithContentDescription("Dünya turu").assertIsFocused()
    }

    @Test fun withoutTourMenuIsAsBefore() {
        menu(withTour = false)
        assertEquals(listOf("Devam et, Türkiye, 1 / 20", "Nasıl oynanır", "Ayarlar"), clickLabels())
    }

    @Test fun gameHeaderNamesPlaceInOneNodeAndShowsVisibleText() {
        val state = FakeStates.ready().copy(place = PlacePosition(TestPlaces.turkey, 0, 3, 20))
        rule.setContent { KelimeTheme { GameContent(state, RecordingCallbacks().callbacks) } }
        val cd = "Seviye 3, Türkiye 3 / 20. 50 jeton. Kalan kelime: 2. Bonus sayacı: 2 / 5"
        val h = rule.onNode(isHeading() and androidx.compose.ui.test.hasContentDescription(cd)).fetchSemanticsNode()
        assertNull("görsel metin ikinci kez okunmamalı", h.config.getOrNull(SemanticsProperties.Text))
    }

    @Test fun howToPlayExplainsWorldTour() {
        val text = HowToPlayParagraphs.joinToString(" ")
        assertTrue(text.contains("Dünya turu") && text.contains("100 ülke") && text.contains("25 jeton"))
    }
}
