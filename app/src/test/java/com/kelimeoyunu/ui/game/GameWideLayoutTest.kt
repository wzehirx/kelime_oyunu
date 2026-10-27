package com.kelimeoyunu.ui.game

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.kelimeoyunu.domain.GameEconomy
import com.kelimeoyunu.ui.assertNoUnlabeledClickables
import com.kelimeoyunu.ui.theme.KelimeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** >= 600 dp ya da yatay: iki sütun; okuma sırası (ızgara/liste, kelime, harfler, eylemler) değişmez. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h480dp")
class GameWideLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val rec = RecordingCallbacks()

    @Test fun twoColumnLayoutKeepsReadingOrderAndSemantics() {
        rule.setContent { KelimeTheme { GameContent(FakeStates.ready("ka", setOf(0, 1)), rec.callbacks) } }
        val labels = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .map { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() }
        assertEquals(
            listOf("Harf K, seçildi", "Harf A, seçildi", "Harf L", "Harf E", "Harf M", "Gönder", "Sil", "Temizle", "Karıştır",
                GameEconomy.HINT_LETTER_DESCRIPTION, GameEconomy.HINT_WORD_DESCRIPTION, "Ana menüye dön"),
            labels,
        )
        rule.onNode(isHeading()).assertIsFocused()
        fun pos(cd: String) = rule.onNodeWithContentDescription(cd).fetchSemanticsNode().positionInRoot
        // Sol sütun (kelime listesi) sağ sütundan (oluşturulan kelime, çark) solda.
        assertTrue(pos("Kelime 1, 3 harfli, bulundu: KAL").x < pos("Oluşturulan kelime: KA").x)
        assertTrue(pos("Oluşturulan kelime: KA").y < pos("Harf K, seçildi").y)
        assertNoUnlabeledClickables(rule)
    }
}
