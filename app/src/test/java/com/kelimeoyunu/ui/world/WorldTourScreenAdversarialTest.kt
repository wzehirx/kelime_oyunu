package com.kelimeoyunu.ui.world

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToIndex
import com.kelimeoyunu.TestPlaces
import com.kelimeoyunu.domain.WorldTour
import com.kelimeoyunu.ui.theme.KelimeTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class WorldTourScreenAdversarialTest {
    @get:Rule val rule = createComposeRule()
    private val tour = WorldTour(TestPlaces.list(100), 10_000)

    private fun show(current: Int) = rule.setContent {
        KelimeTheme { WorldTourScreen(tour, current, { }, { }) }
    }

    @Test fun BUG_levelTwentyDoneButNextNotPressedListMustNotSayStillInProgress19of20() {
        // Oyun "Türkiye tamamlandı, 25 jeton" dedi; kayıtlı index hâlâ 19. Liste 19/20 ve "şu an buradasın" diyor.
        show(19)
        rule.onNodeWithContentDescription("Türkiye, tamamlandı", substring = true).assertExists()
    }

    @Test fun listIsOneCollectionAndLastRowIsReachable() {
        show(0)
        val list = rule.onNode(hasScrollAction()).fetchSemanticsNode()
        val info = list.config.getOrNull(SemanticsProperties.CollectionInfo)
        assertNotNull("LazyColumn collectionInfo", info)
        // Not: Compose LazyColumn satır sayısını bildirmez (rowCount=${info!!.rowCount}); başlık "x / 100 ülke" der.
        rule.onNode(hasScrollAction()).performScrollToIndex(99)
        rule.onNodeWithContentDescription("Ülke 99", substring = true).assertExists()
    }

    @Test fun currentRowHoldsFocusNearTheEndOfTheTour() {
        show(9_999)
        rule.onNodeWithContentDescription("Ülke 99, ${tour.levelCountOf(99) - 1} / ${tour.levelCountOf(99)} seviye, şu an buradasın", substring = true).assertIsFocused()
    }

    @Test fun collectionInfoReportsTheRealRowCount() {
        show(0)
        val info = rule.onNode(hasScrollAction()).fetchSemanticsNode().config.getOrNull(SemanticsProperties.CollectionInfo)!!
        org.junit.Assert.assertEquals(100, info.rowCount)
    }

    @Test fun savedLevelDoneCountsAsProgress() {
        rule.setContent { KelimeTheme { WorldTourScreen(tour, 19, { }, { }, savedLevelDone = true) } }
        rule.onNodeWithContentDescription("Türkiye, tamamlandı", substring = true).assertExists()
        rule.onNodeWithContentDescription("Dünya turu, 1 / 100 ülke tamamlandı").assertExists()
        rule.onNodeWithContentDescription("Türkiye, 19 / 20", substring = true).assertDoesNotExist()
    }

    @Test fun savedLevelNotDoneKeepsInProgress() {
        rule.setContent { KelimeTheme { WorldTourScreen(tour, 19, { }, { }, savedLevelDone = false) } }
        rule.onNodeWithContentDescription("Türkiye, 19 / 20 seviye, şu an buradasın").assertExists()
        rule.onNodeWithContentDescription("Dünya turu, 0 / 100 ülke tamamlandı").assertExists()
    }
}
