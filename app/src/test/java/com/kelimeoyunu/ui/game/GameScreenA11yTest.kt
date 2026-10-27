package com.kelimeoyunu.ui.game

import com.kelimeoyunu.domain.GameEconomy
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
import kotlin.math.atan2
import androidx.compose.ui.test.assertIsEnabled
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
        "Gönder", "Sil", "Temizle", "Karıştır", GameEconomy.HINT_LETTER_DESCRIPTION, GameEconomy.HINT_WORD_DESCRIPTION,
    )
    private val menuLabel = "Ana menüye dön"

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
            listOf("Harf K", "Harf A", "Harf L", "Harf E", "Harf M") + actionLabels + menuLabel,
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
            y(GameEconomy.HINT_WORD_DESCRIPTION),
        )
        assertEquals(ys.sorted(), ys)
    }

    // v2.0: harfler 4'lü satırlar yerine pusula halkasında; ilk jeton saat 12'de, eşit aralıklı, ortada Karıştır.
    @Test fun lettersSitOnACompassRingStartingAtTwelveWithShuffleInTheMiddle() {
        show()
        fun center(cd: String) = rule.onNodeWithContentDescription(cd).fetchSemanticsNode().boundsInRoot.center
        val middle = center("Karıştır")
        val points = "KALEM".map { center("Harf $it") }
        val radii = points.map { (it - middle).getDistance() }
        radii.forEach { assertEquals("jetonlar aynı halkada olmalı", radii[0], it, 1.5f) }
        assertEquals("ilk jeton saat 12 yönünde", middle.x, points[0].x, 1.5f)
        assertTrue(points[0].y < middle.y)
        assertTrue("ikinci jeton saat yönünde (sağda)", points[1].x > middle.x)
        val angles = points.map { Math.toDegrees(atan2((it.y - middle.y).toDouble(), (it.x - middle.x).toDouble())) }
        for (i in 1 until angles.size) {
            val step = ((angles[i] - angles[i - 1]) + 360.0) % 360.0
            assertEquals("5 harfte jetonlar 72 derece arayla", 72.0, step, 1.0)
        }
    }

    // Okuma sırasını geometri değil açık traversalIndex belirler (çarkta geometri saat yönüdür).
    @Test fun traversalIndicesKeepTheOldReadingOrder() {
        show()
        fun idx(cd: String) = rule.onNodeWithContentDescription(cd).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.TraversalIndex)!!
        val order = listOf("Harf K", "Harf A", "Harf L", "Harf E", "Harf M") + actionLabels
        val values = order.map { idx(it) }
        assertEquals("traversalIndex okuma sırasını vermeli", values.sorted(), values)
        assertEquals("indeksler benzersiz", values.size, values.toSet().size)
    }

    @Test fun crosswordGridIsHiddenFromAccessibilityAndWordListCarriesTheInfo() {
        val grid = GridUi(
            2, 3,
            listOf(
                GridCellUi(0, 0, 'k', false), GridCellUi(0, 1, 'a', false), GridCellUi(0, 2, 'l', false),
                GridCellUi(1, 1, 'e', true), GridCellUi(1, 2, null, false),
            ),
        )
        show(FakeStates.ready().copy(grid = grid))
        // Izgara harfleri ağaçta yok (kelime listesi satırları etiketle okunur, görsel harf metni yok).
        listOf("K", "A", "L", "E").forEach { rule.onAllNodes(androidx.compose.ui.test.hasText(it)).assertCountEquals(0) }
        rule.onNodeWithContentDescription("Kelime 1, 3 harfli, bulundu: KAL").assertExists()
        assertUnlabeledClickables()
    }

    @Test fun selectedTilesShowOrderBadgesOnlyVisually() {
        val s = FakeStates.ready("ka", setOf(0, 1)).copy(selection = listOf(1, 0))
        show(s)
        // Rozet metni anlamsal ağaçta yok; etiket yalnızca "seçildi" der.
        rule.onAllNodes(androidx.compose.ui.test.hasText("1")).assertCountEquals(0)
        rule.onNodeWithContentDescription("Harf K, seçildi").assertHasClickAction()
        rule.onNodeWithContentDescription("Harf A, seçildi").assertHasClickAction()
    }

    @Test fun emptySubmitStaysEnabledAndReportsThroughTheCallback() {
        show()
        rule.onNodeWithContentDescription("Gönder").assertIsEnabled().performScrollTo().performClick()
        assertEquals(listOf("submit"), rec.calls)
    }

    @Test fun menuButtonIsExposedLastWithItsOwnLabel() {
        val calls = ArrayList<String>()
        rule.setContent { KelimeTheme { GameContent(FakeStates.ready(), rec.callbacks.copy(onMenu = { calls += "menu" })) } }
        assertEquals(menuLabel, clickLabels().last())
        rule.onNodeWithContentDescription(menuLabel).assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(listOf("menu"), calls)
        rule.onAllNodes(androidx.compose.ui.test.hasText("Menü")).assertCountEquals(0)
        // Açık traversalIndex: menü en sonda okunur.
        val menuIdx = rule.onNodeWithContentDescription(menuLabel).fetchSemanticsNode().config.getOrNull(SemanticsProperties.TraversalIndex)!!
        val lastAction = rule.onNodeWithContentDescription(GameEconomy.HINT_WORD_DESCRIPTION).fetchSemanticsNode().config.getOrNull(SemanticsProperties.TraversalIndex)!!
        assertTrue(menuIdx > lastAction)
    }

    // Karıştır sonrası okuma sırası, tiles listesi sırasıyla aynıdır (havuz kimliğiyle değil).
    @Test fun traversalFollowsTheShuffledTileListOrder() {
        val base = FakeStates.ready()
        val shuffled = base.copy(tiles = listOf(3, 1, 4, 0, 2).map { base.tiles[it] })
        show(shuffled)
        val letters = shuffled.tiles.map { "Harf ${it.char.uppercaseChar()}" }
        val values = letters.map {
            rule.onNodeWithContentDescription(it).fetchSemanticsNode().config.getOrNull(SemanticsProperties.TraversalIndex)!!
        }
        assertEquals(values.sorted(), values)
        assertEquals(values.size, values.toSet().size)
        val send = rule.onNodeWithContentDescription("Gönder").fetchSemanticsNode().config.getOrNull(SemanticsProperties.TraversalIndex)!!
        assertTrue(send > values.last())
    }

    @Test fun wheelDoesNotShakeOnItsOwnWhenAnOldShakeCountExists() {
        val effects = GameEffects().also { it.onEvent(GameEvent.Submitted(com.kelimeoyunu.domain.SubmitResult.Invalid("x"))) }
        rule.mainClock.autoAdvance = false
        rule.setContent { KelimeTheme { GameContent(FakeStates.ready(), rec.callbacks, effects = effects) } }
        rule.mainClock.advanceTimeBy(50)
        val x0 = rule.onNodeWithContentDescription("Harf K").fetchSemanticsNode().positionInRoot.x
        rule.mainClock.advanceTimeBy(60)
        val x1 = rule.onNodeWithContentDescription("Harf K").fetchSemanticsNode().positionInRoot.x
        assertEquals("eski sayaç çarkı sallamamalı", x0, x1, 0.01f)
        // Yeni geçersiz gönderim sallar.
        effects.onEvent(GameEvent.Submitted(com.kelimeoyunu.domain.SubmitResult.Invalid("y")))
        var maxMove = 0f
        repeat(12) {
            rule.mainClock.advanceTimeBy(16)
            maxMove = maxOf(maxMove, kotlin.math.abs(rule.onNodeWithContentDescription("Harf K").fetchSemanticsNode().positionInRoot.x - x1))
        }
        assertTrue("yeni olay çarkı sallamalı", maxMove > 0.5f)
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
        rule.onNodeWithContentDescription(GameEconomy.HINT_WORD_DESCRIPTION).performScrollTo().performClick()
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
