package com.kelimeoyunu.ui.game

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertCountEquals
import com.kelimeoyunu.ui.assertNoUnlabeledClickables
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
class GameScreenA11yTest {
    @get:Rule val rule = createComposeRule()

    private val rec = RecordingCallbacks()
    private val actionLabels = listOf(
        "Gönder", "Sil", "Temizle", "Karıştır", "İpucu, harf aç, 10 jeton", "Kelime aç, 30 jeton",
    )

    private fun show(state: GameUiState.Ready = FakeStates.ready(), fontScale: Float = 1f) {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                KelimeTheme { GameContent(state, rec.callbacks) }
            }
        }
    }

    private fun clickLabels(): List<String?> = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        .map { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() }

    @Test fun everyLetterButtonIsLabeledClickableAndTallEnough() {
        show()
        "KALEM".forEach {
            rule.onNodeWithContentDescription("Harf $it").assertHasClickAction().assertHeightIsAtLeast(48.dp)
        }
    }

    @Test fun usedLetterSaysSelected() {
        show(FakeStates.ready("k", setOf(0)))
        rule.onNodeWithContentDescription("Harf K, seçildi").assertHasClickAction()
        rule.onNodeWithContentDescription("Harf A").assertHasClickAction()
    }

    @Test fun clickableOrderIsLettersThenActionsInFixedOrder() {
        show()
        assertEquals(
            listOf("Harf K", "Harf A", "Harf L", "Harf E", "Harf M") + actionLabels,
            clickLabels(),
        )
    }

    @Test fun actionButtonsAreLabeledAndTall() {
        show()
        actionLabels.forEach { rule.onNodeWithContentDescription(it).assertHasClickAction().assertHeightIsAtLeast(48.dp) }
    }

    @Test fun wordRowsCarryDescriptions() {
        show()
        rule.onNodeWithContentDescription("Kelime 1, 3 harfli, bulundu: KAL").assertExists()
        rule.onNodeWithContentDescription("Kelime 2, 5 harfli, açık harfler: _ A _ E _").assertExists()
        rule.onNodeWithContentDescription("Kelime 3, 4 harfli, açık harfler: _ _ _ _").assertExists()
    }

    @Test fun currentWordFieldIsLabeledAndNotALiveRegion() {
        show(FakeStates.ready("kal", setOf(0, 1, 2)))
        val node = rule.onNodeWithContentDescription("Oluşturulan kelime: KAL").fetchSemanticsNode()
        assertNull("liveRegion kullanılmamalı (Announcer konuşuyor)", node.config.getOrNull(SemanticsProperties.LiveRegion))
    }

    @Test fun emptyCurrentWordSaysEmpty() {
        show()
        rule.onNodeWithContentDescription("Oluşturulan kelime: boş").assertExists()
    }

    @Test fun headerIsHeadingWithSingleDescription() {
        show()
        val h = rule.onNode(isHeading() and hasContentDescription("Seviye 3. 50 jeton. Kalan kelime: 2. Bonus sayacı: 2 / 5"))
        val node = h.fetchSemanticsNode()
        assertNull("görsel metin ikinci kez okunmamalı", node.config.getOrNull(SemanticsProperties.Text))
    }

    @Test fun headerGetsInitialFocus() {
        show()
        rule.onNode(isHeading()).assertIsFocused()
    }

    @Test fun readingOrderIsTopToBottom() {
        show()
        fun y(cd: String) = rule.onNodeWithContentDescription(cd).fetchSemanticsNode().positionInRoot.y
        val ys = listOf(
            y("Seviye 3. 50 jeton. Kalan kelime: 2. Bonus sayacı: 2 / 5"),
            y("Kelime 1, 3 harfli, bulundu: KAL"),
            y("Kelime 3, 4 harfli, açık harfler: _ _ _ _"),
            y("Oluşturulan kelime: boş"),
            y("Harf K"),
            y("Gönder"),
            y("Kelime aç, 30 jeton"),
        )
        assertEquals(ys.sorted(), ys)
    }

    @Test fun lettersWrapInRowsOfFour() {
        show()
        fun y(cd: String) = rule.onNodeWithContentDescription(cd).fetchSemanticsNode().positionInRoot.y
        assertEquals(y("Harf K"), y("Harf E"), 0.5f)
        assertTrue(y("Harf M") > y("Harf K"))
    }

    @Test fun clicksCallTheRightCallbacks() {
        show()
        rule.onNodeWithContentDescription("Harf L").performScrollTo().performClick()
        actionLabels.forEach { rule.onNodeWithContentDescription(it).performScrollTo().performClick() }
        assertEquals(
            listOf("letter2", "submit", "delete", "clear", "shuffle", "hintLetter", "hintWord"),
            rec.calls,
        )
    }

    @Test fun fontScale2KeepsEverythingReachableAndTall() {
        show(fontScale = 2f)
        (listOf("Harf K", "Harf M") + actionLabels).forEach {
            rule.onNodeWithContentDescription(it).assertHasClickAction().assertHeightIsAtLeast(48.dp)
        }
        rule.onNodeWithContentDescription("Kelime aç, 30 jeton").performScrollTo().performClick()
        assertEquals(listOf("hintWord"), rec.calls)
    }

    @Test fun noClickableNodeIsUnlabeled() {
        show(FakeStates.ready("ka", setOf(0, 1)))
        assertUnlabeledClickables()
    }

    @Test fun levelCompleteShowsNextButtonWithFocusAndSummary() {
        show(FakeStates.ready(complete = true))
        rule.onNodeWithContentDescription("Sonraki seviye").assertHasClickAction().assertIsFocused()
        rule.onAllNodesWithContentDescription("Gönder").assertCountEquals(0)
        rule.onNode(isHeading() and androidx.compose.ui.test.hasText("Seviye 3 tamamlandı")).assertExists()
        rule.onNodeWithContentDescription("Sonraki seviye").performClick()
        assertEquals(listOf("next"), rec.calls)
        assertUnlabeledClickables()
    }

    @Test fun allDoneHasMenuButtonWithFocus() {
        var back = 0
        rule.setContent { KelimeTheme { AllDoneContent({ back++ }) } }
        rule.onNodeWithContentDescription("Ana menüye dön").assertHasClickAction().assertIsFocused().performClick()
        assertEquals(1, back)
        assertUnlabeledClickables()
    }

    private fun assertUnlabeledClickables() = assertNoUnlabeledClickables(rule)
}
