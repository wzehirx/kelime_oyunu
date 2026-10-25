package com.kelimeoyunu

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.kelimeoyunu.ui.game.GameUiState
import com.kelimeoyunu.ui.game.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppContainerTest {
    @Test
    fun factoryBuildsViewModelThatLoadsRealAssets() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = AppContainer(app).gameViewModelFactory().create(GameViewModel::class.java)
        val s = runBlocking {
            withContext(Dispatchers.Default) {
                withTimeout(60_000) { vm.state.first { it !is GameUiState.Loading } }
            }
        }
        assertTrue("durum: $s", s is GameUiState.Ready)
        s as GameUiState.Ready
        assertEquals(1, s.levelNumber)
        assertEquals(50, s.coins)
    }
}
