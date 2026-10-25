package com.kelimeoyunu.ui.menu

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.ui.assertNoUnlabeledClickables
import com.kelimeoyunu.ui.settings.SettingsScreen
import com.kelimeoyunu.ui.theme.KelimeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class MainMenuA11yTest {
    @get:Rule val rule = createComposeRule()
    private val calls = ArrayList<String>()

    private fun showMenu(level: Int? = 7) = rule.setContent {
        KelimeTheme { MainMenuScreen(level, { calls += "continue" }, { calls += "howto" }, { calls += "settings" }) }
    }

    @Test fun continueButtonNamesLevelAndHasInitialFocus() {
        showMenu()
        rule.onNodeWithContentDescription("Devam et, Seviye 7").assertHasClickAction().assertIsFocused()
    }

    @Test fun continueWithoutProgressIsPlain() {
        showMenu(null)
        rule.onNodeWithContentDescription("Devam et").assertHasClickAction()
    }

    @Test fun menuButtonsAreOrderedAndTall() {
        showMenu()
        val labels = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .map { it.config.getOrNull(SemanticsProperties.ContentDescription)?.first() }
        assertEquals(listOf("Devam et, Seviye 7", "Nasıl oynanır", "Ayarlar"), labels)
        labels.forEach { rule.onNodeWithContentDescription(it!!).assertHeightIsAtLeast(48.dp) }
        rule.onNode(isHeading() and androidx.compose.ui.test.hasText("Kelime Oyunu")).assertExists()
        assertNoUnlabeledClickables(rule)
    }

    @Test fun menuClicksNavigate() {
        showMenu()
        rule.onNodeWithContentDescription("Devam et, Seviye 7").performClick()
        rule.onNodeWithContentDescription("Nasıl oynanır").performClick()
        rule.onNodeWithContentDescription("Ayarlar").performClick()
        assertEquals(listOf("continue", "howto", "settings"), calls)
    }

    @Test fun howToPlayHasHeadingFocusTextAndBack() {
        rule.setContent { KelimeTheme { HowToPlayScreen({ calls += "back" }) } }
        rule.onNode(isHeading()).assertIsFocused()
        HowToPlayParagraphs.forEach { rule.onNodeWithText(it).assertExists() }
        assertTrue(HowToPlayParagraphs.joinToString(" ").contains("Gönder"))
        rule.onNodeWithContentDescription("Geri").performClick()
        assertEquals(listOf("back"), calls)
        assertNoUnlabeledClickables(rule)
    }

    @Test fun settingsSwitchReadsStateAndToggles() {
        var sound by androidx.compose.runtime.mutableStateOf(true)
        rule.setContent {
            KelimeTheme {
                SettingsScreen(sound, { sound = it; calls += "sound$it" }, { calls += "reset" }, { calls += "back" })
            }
        }
        rule.onNode(isHeading()).assertIsFocused()
        fun state() = rule.onNodeWithContentDescription("Ses efektleri").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals("açık", state())
        rule.onNodeWithContentDescription("Ses efektleri").performClick()
        rule.waitForIdle()
        assertEquals("kapalı", state())
        assertEquals(listOf("soundfalse"), calls)
        assertNoUnlabeledClickables(rule)
    }

    @Test fun resetAsksForConfirmation() {
        rule.setContent { KelimeTheme { SettingsScreen(true, {}, { calls += "reset" }, { calls += "back" }) } }
        rule.onNodeWithContentDescription("İlerlemeyi sıfırla").performClick()
        rule.onNodeWithText("İlerleme sıfırlansın mı?").assertExists()
        assertTrue("onaydan önce sıfırlanmamalı", calls.isEmpty())
        rule.onNodeWithText("Vazgeç").performClick()
        assertTrue(calls.isEmpty())
        rule.onNodeWithContentDescription("İlerlemeyi sıfırla").performClick()
        rule.onNodeWithText("Evet, sıfırla").performClick()
        assertEquals(listOf("reset"), calls)
    }
}
