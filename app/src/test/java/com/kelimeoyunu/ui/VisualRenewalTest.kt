package com.kelimeoyunu.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.kelimeoyunu.TestPlaces
import com.kelimeoyunu.domain.GameEconomy
import com.kelimeoyunu.domain.PlacePosition
import com.kelimeoyunu.domain.WorldTour
import com.kelimeoyunu.ui.game.FakeStates
import com.kelimeoyunu.ui.game.GameCallbacks
import com.kelimeoyunu.ui.game.GameContent
import com.kelimeoyunu.ui.game.GridCellUi
import com.kelimeoyunu.ui.game.GridUi
import com.kelimeoyunu.ui.game.RecordingCallbacks
import com.kelimeoyunu.ui.menu.MainMenuScreen
import com.kelimeoyunu.ui.settings.SettingsScreen
import com.kelimeoyunu.ui.theme.KelimeTheme
import com.kelimeoyunu.ui.world.WorldTourScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** v2.0 görsel yenileme: yeni görsel yapı eski anlamsal sözleşmeyi bozmaz ve süs öğeleri ağaçta yoktur. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w320dp-h568dp")
class VisualRenewalTest {
    @get:Rule val rule = createComposeRule()
    private val calls = ArrayList<String>()
    private var fontScale by mutableStateOf(1f)
    private var density = 1f
    private val pos = PlacePosition(TestPlaces.turkey, 0, 3, 20)

    private fun host(dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) = rule.setContent {
        val d = LocalDensity.current
        density = d.density
        CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) { KelimeTheme(darkTheme = dark) { content() } }
    }

    private fun label(n: SemanticsNode): String =
        n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString("")

    private fun scanNoOverflowAndTargets(ctx: String) {
        val rootW = rule.onRoot().fetchSemanticsNode().size.width.toFloat()
        rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { n ->
            assertTrue("$ctx: etiketsiz tıklanabilir ${n.config}", label(n).isNotBlank())
            assertTrue("$ctx: ${label(n)} < 48dp", n.size.width / density >= 48f && n.size.height / density >= 48f)
        }
        rule.onAllNodes(isRoot().not()).fetchSemanticsNodes().forEach {
            assertTrue("$ctx: taşma ${it.boundsInRoot}", it.boundsInRoot.left >= -1f && it.boundsInRoot.right <= rootW + 1f)
        }
    }

    // ---- ana menü ----

    private fun fullMenu() = host {
        MainMenuScreen(
            3, { calls += "continue" }, { calls += "howto" }, { calls += "settings" },
            place = pos, onWorldTour = { calls += "world" },
            coins = 50, totalLevels = 10_000, worldCompleted = 12, worldTotal = 100,
        )
    }

    @Test fun heroPanelIsOneNodeAndKeepsTheContinueLabel() {
        fullMenu()
        val label = "Devam et, Türkiye, 3 / 20"
        val node = rule.onNodeWithContentDescription(label).assertHasClickAction().assertIsFocused().fetchSemanticsNode()
        assertTrue("kahraman panelin görsel metni ikinci kez okunmamalı", node.config.getOrNull(SemanticsProperties.Text).isNullOrEmpty())
        rule.onAllNodes(hasText("Seviye 3")).assertCountEquals(0)
        rule.onAllNodes(hasText("Devam et")).assertCountEquals(0)
        rule.onNodeWithContentDescription(label).performClick()
        assertEquals(listOf("continue"), calls)
    }

    @Test fun menuRowsKeepOrderLabelsAndHideDecoration() {
        fullMenu()
        val labels = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { label(it) }
        assertEquals(listOf("Devam et, Türkiye, 3 / 20", "Dünya turu", "Nasıl oynanır", "Ayarlar"), labels)
        rule.onAllNodes(hasText("12 / 100 ülke")).assertCountEquals(0)
        // Altbilgi tek anlamsal satırdır; Dünya turu/Devam et etiketleri değişmedi (yukarıdaki sıra testi).
        val footer = rule.onNodeWithContentDescription(
            "Jeton: 50. Toplam seviye: 10000. Ülke ilerlemesi: Türkiye 3 / 20. Dünya turu: 12 / 100 ülke tamamlandı",
        ).fetchSemanticsNode()
        assertTrue(footer.config.getOrNull(SemanticsProperties.Role) == null)
        rule.onAllNodes(isHeading() and hasText("Kelime Oyunu")).assertCountEquals(1)
        assertNoUnlabeledClickables(rule)
    }

    @Test fun menuHasNoOverflowAtFontScale2WithEveryExtra() {
        fullMenu()
        for (s in listOf(1f, 2f)) {
            fontScale = s
            rule.waitForIdle()
            scanNoOverflowAndTargets("menü @$s")
        }
    }

    @Test fun menuRendersInDarkTheme() {
        host(dark = true) { MainMenuScreen(null, {}, {}, {}) }
        rule.onNodeWithContentDescription("Devam et").assertHasClickAction()
    }

    // ---- ayarlar ----

    @Test fun settingsHasThreeSwitchRowsThenDangerThenBackInOrder() {
        var sound by mutableStateOf(true)
        var vibration by mutableStateOf(true)
        var animations by mutableStateOf(true)
        host {
            SettingsScreen(
                sound, { sound = it }, { calls += "reset" }, { calls += "back" },
                vibrationEnabled = vibration, onVibrationChange = { vibration = it },
                animationsEnabled = animations, onAnimationsChange = { animations = it },
            )
        }
        assertEquals(
            listOf("Ses efektleri", "Titreşim", "Animasyonlar", "İlerlemeyi sıfırla", "Geri"),
            rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { label(it) },
        )
        fun state(name: String) = rule.onNodeWithContentDescription(name).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals("açık", state("Titreşim"))
        rule.onNodeWithContentDescription("Titreşim").performClick()
        rule.onNodeWithContentDescription("Animasyonlar").performClick()
        rule.waitForIdle()
        assertEquals("kapalı", state("Titreşim"))
        assertEquals("kapalı", state("Animasyonlar"))
        assertEquals("açık", state("Ses efektleri"))
        assertTrue(!vibration && !animations && sound)
        assertNoUnlabeledClickables(rule)
    }

    @Test fun settingsHasNoOverflowAtFontScale2() {
        host { SettingsScreen(true, {}, {}, {}) }
        for (s in listOf(1f, 2f)) {
            fontScale = s
            rule.waitForIdle()
            scanNoOverflowAndTargets("ayarlar @$s")
        }
    }

    // ---- oyun ----

    private fun gridState() = FakeStates.ready().copy(
        grid = GridUi(
            3, 5,
            List(5) { GridCellUi(0, it, if (it < 3) "kalem"[it] else null, false) } +
                List(3) { GridCellUi(it, 1, if (it == 2) 'l' else null, it == 2) },
        ),
        place = pos,
        selection = emptyList(),
    )

    @Test fun gameKeepsEverySemanticAtFontScale2WithGridAndPlaceHeader() {
        val rec = RecordingCallbacks()
        host { GameContent(gridState(), rec.callbacks) }
        val expected = listOf("Harf K", "Harf A", "Harf L", "Harf E", "Harf M", "Gönder", "Sil", "Temizle", "Karıştır") +
            listOf(GameEconomy.HINT_LETTER_DESCRIPTION, GameEconomy.HINT_WORD_DESCRIPTION, "Ana menüye dön")
        for (s in listOf(1f, 2f)) {
            fontScale = s
            rule.waitForIdle()
            assertEquals("@$s", expected, rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { label(it) })
            scanNoOverflowAndTargets("oyun @$s")
        }
        rule.onNode(isHeading() and androidx.compose.ui.test.hasContentDescription("Seviye 3, Türkiye 3 / 20. 50 jeton. Kalan kelime: 2. Bonus sayacı: 2 / 5")).assertIsFocused()
    }

    @Test fun gameRendersInDarkTheme() {
        host(dark = true) { GameContent(gridState(), GameCallbacks()) }
        rule.onNodeWithContentDescription("Harf K").assertHasClickAction()
    }

    @Test fun levelEndWithCountryFinishKeepsFocusAndSemantics() {
        val last = pos.copy(levelInPlace = 20)
        host { GameContent(FakeStates.ready(complete = true).copy(place = last), GameCallbacks(onNextLevel = { calls += "next" })) }
        rule.onNodeWithContentDescription("Sonraki seviye").assertHasClickAction().assertIsFocused().performClick()
        assertEquals(listOf("next"), calls)
        rule.onAllNodes(hasText("TÜ")).assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Gönder").assertCountEquals(0)
    }

    // ---- dünya turu ----

    @Test fun worldRowsKeepLabelsAndHideStatusPillsAndStamps() {
        val tour = WorldTour(TestPlaces.list(100), 10_000)
        host { WorldTourScreen(tour, 205, { calls += it }, {}) }
        rule.onNodeWithContentDescription("Ülke 10, 5 / 50 seviye, şu an buradasın").assertHasClickAction().assertIsFocused()
        rule.onAllNodes(hasText("Kilitli")).assertCountEquals(0)
        rule.onAllNodes(hasText("Devam")).assertCountEquals(0)
        rule.onAllNodes(hasText("Tamamlandı")).assertCountEquals(0)
        val row = rule.onNodeWithContentDescription("Ülke 10, 5 / 50 seviye, şu an buradasın").fetchSemanticsNode()
        assertNull(row.config.getOrNull(SemanticsProperties.Text))
        for (s in listOf(1f, 2f)) {
            fontScale = s
            rule.waitForIdle()
            scanNoOverflowAndTargets("dünya @$s")
        }
    }

    // ---- eylem alanı alta sabit (yüksek ekran) ----

    @Test @Config(qualifiers = "w360dp-h800dp") fun tallScreenShowsWheelAndSubmitOnTheFirstScreen() {
        host { GameContent(gridState(), RecordingCallbacks().callbacks) }
        val rootH = rule.onRoot().fetchSemanticsNode().size.height.toFloat()
        for (cd in listOf("Harf K", "Harf M", "Gönder", "Karıştır", "Sil", "Temizle")) {
            val b = rule.onNodeWithContentDescription(cd).fetchSemanticsNode().boundsInRoot
            assertTrue("$cd ilk ekranda görünmeli: $b / $rootH", b.top >= 0f && b.bottom <= rootH + 1f)
        }
        // Üst kısım kaydırılabilir, okuma sırası ve etiketler aynı.
        assertEquals(
            listOf("Harf K", "Harf A", "Harf L", "Harf E", "Harf M", "Gönder", "Sil", "Temizle", "Karıştır",
                GameEconomy.HINT_LETTER_DESCRIPTION, GameEconomy.HINT_WORD_DESCRIPTION, "Ana menüye dön"),
            rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { label(it) },
        )
    }
}
