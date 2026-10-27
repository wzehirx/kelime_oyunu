package com.kelimeoyunu.ui.game

import com.kelimeoyunu.domain.GameEconomy
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.domain.WordDictionary
import com.kelimeoyunu.ui.theme.KelimeTheme
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Gerçek ViewModel ile: bir seviye yalnızca etiketli düğmelere dokunularak (ekrana bakmadan) bitirilebilir. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class GameScreenFlowTest {
    @get:Rule val rule = createComposeRule()
    private val dir = kotlin.io.path.createTempDirectory("flow").toFile()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @After fun tearDown() { scope.cancel(); dir.deleteRecursively() }

    private fun gz(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }

    private fun tap(cd: String) = rule.onNodeWithContentDescription(cd).performScrollTo().performClick()

    @Test fun levelCanBeFinishedUsingOnlyLabeledButtons() {
        val repo = ProgressRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "p.preferences_pb") })
        val levels = LevelRepository { ByteArrayInputStream(gz("1|ten|ten,0,0,y;net,0,2,d\n2|etm|met,0,0,y;tem,0,2,d\n")) }
        val vm = GameViewModel(levels, WordDictionary(setOf("ten", "net", "et")), repo, Random(1), Dispatchers.Default)
        val events = CopyOnWriteArrayList<GameEvent>()
        rule.setContent {
            KelimeTheme { GameScreen(vm, { events += it }, onExitToMenu = {}) }
        }
        rule.waitUntil(30_000) { rule.onAllNodes(hasContentDescription("Harf T")).fetchSemanticsNodes().isNotEmpty() }

        tap("Harf T"); tap("Harf E"); tap("Harf N")
        rule.onNodeWithContentDescription("Oluşturulan kelime: TEN").assertExists()
        tap("Harf T, seçildi") // ikinci kez seçim etkisiz
        tap("Gönder")
        rule.waitUntil(10_000) {
            rule.onAllNodes(hasContentDescription("Kelime 1, 3 harfli, bulundu: TEN")).fetchSemanticsNodes().isNotEmpty()
        }
        tap("Harf N"); tap("Harf E"); tap("Harf T"); tap("Gönder")

        rule.waitUntil(10_000) { rule.onAllNodes(hasContentDescription("Sonraki seviye")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Sonraki seviye").assertIsFocused()
        assertTrue(events.any { it == GameEvent.Submitted(SubmitResult.Correct("ten")) })
        assertTrue(events.any { it == GameEvent.LevelCompleted() })
        assertEquals(1, events.count { it == GameEvent.LevelCompleted() })

        tap("Sonraki seviye")
        rule.waitUntil(10_000) {
            rule.onAllNodes(hasContentDescription("Seviye 2. ${50 + GameEconomy.LEVEL_REWARD} jeton. Kalan kelime: 2. Bonus sayacı: 0 / 5")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
