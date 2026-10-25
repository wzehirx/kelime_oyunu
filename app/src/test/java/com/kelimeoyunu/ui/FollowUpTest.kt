package com.kelimeoyunu.ui

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.AllDoneContent
import com.kelimeoyunu.ui.game.DoubleSubmitGate
import com.kelimeoyunu.ui.game.FakeStates
import com.kelimeoyunu.ui.game.GameContent
import com.kelimeoyunu.ui.game.GameEvent
import com.kelimeoyunu.ui.game.RecordingCallbacks
import com.kelimeoyunu.ui.menu.MainMenuScreen
import com.kelimeoyunu.ui.theme.KelimeTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FollowUpTest {
    @get:Rule val rule = createComposeRule()

    @Test fun reopenedCompletedLevelFocusesHeadingFirst() {
        rule.setContent { KelimeTheme { GameContent(FakeStates.ready(complete = true), RecordingCallbacks().callbacks, levelCompleteHeadingFirst = true) } }
        rule.onNodeWithText("Seviye 3 tamamlandı").assertIsFocused()
        rule.onNodeWithContentDescription("Sonraki seviye").assertExists()
    }

    @Test fun allDoneFromMenuFocusesHeadingWithLevelCount() {
        rule.setContent { KelimeTheme { AllDoneContent({}, levelCount = 10_000, headingFirst = true) } }
        rule.onNodeWithContentDescription("Tebrikler! Tüm seviyeleri tamamladın. Toplam 10000 seviye.").assertIsFocused()
    }

    @Test fun menuFocusReturnsToSettingsAndAllDoneLabelHasNoLevelNumber() {
        rule.setContent { KelimeTheme { MainMenuScreen(10_001, {}, {}, {}, allDone = true, focusSettings = true) } }
        rule.onNodeWithContentDescription("Ayarlar").assertIsFocused()
        rule.onNodeWithContentDescription("Devam et, tüm seviyeler tamamlandı").assertExists()
    }

    @Test fun gateSuppressesEmptyOnlyShortlyAfterARealResult() {
        var t = 0L
        val g = DoubleSubmitGate({ t }, 800)
        val empty = GameEvent.Submitted(SubmitResult.Empty)
        assertFalse(g.shouldSuppress(empty))
        assertFalse(g.shouldSuppress(GameEvent.Submitted(SubmitResult.Invalid("x"))))
        t = 799; assertTrue(g.shouldSuppress(empty))
        t = 801; assertFalse(g.shouldSuppress(empty))
        assertFalse(g.shouldSuppress(GameEvent.Deleted))
    }
}
