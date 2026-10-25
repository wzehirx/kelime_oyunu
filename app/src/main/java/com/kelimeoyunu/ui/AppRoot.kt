package com.kelimeoyunu.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.kelimeoyunu.AppContainer
import com.kelimeoyunu.a11y.AnnouncingFeedbackSink
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.Announcer
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.ui.game.CompositeFeedbackSink
import com.kelimeoyunu.ui.game.GameScreen
import com.kelimeoyunu.ui.game.GameUiState
import com.kelimeoyunu.ui.game.GameViewModel
import com.kelimeoyunu.ui.game.SoundFeedbackSink
import com.kelimeoyunu.ui.menu.HowToPlayScreen
import com.kelimeoyunu.ui.menu.MainMenuScreen
import com.kelimeoyunu.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Menu, Game, HowToPlay, Settings }

/**
 * Basit durum tabanlı gezinme. Menü dışındaki ekranlardan geri tuşu menüye döner. Ses ayarı akışı ve
 * [AppContainer.soundManager] uygulama düzeyindedir: menüdeyken ayar değişimi hemen etki eder, oyun
 * ekranı aynı SoundManager'ı kullanır. Her ekran geçişinde (ses açıksa) [Sfx.NAV] çalar.
 */
@Composable
fun AppRoot(container: AppContainer, modifier: Modifier = Modifier) {
    var screen by rememberSaveable { mutableStateOf(Screen.Menu) }
    var previous by rememberSaveable { mutableStateOf<Screen?>(null) }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val announcer = remember { Announcer({ view.announceForAccessibility(it) }, scope) }

    // İlk değer gelene kadar SoundManager.enabled = false (sessiz).
    val soundSetting by container.settingsRepository.soundEnabled.collectAsState(initial = null)
    SideEffect { container.soundManager.enabled = soundSetting == true }

    fun go(next: Screen) {
        container.soundManager.play(Sfx.NAV)
        previous = screen
        screen = next
    }
    BackHandler(enabled = screen != Screen.Menu) { go(Screen.Menu) }

    when (screen) {
        Screen.Menu -> {
            val progress by container.progressRepository.progress.collectAsState(initial = null)
            val levelCount by produceState<Int?>(null, container) {
                value = withContext(Dispatchers.Default) { container.levelRepository.count }
            }
            val p = progress
            val count = levelCount
            // İkisi de okunana kadar seviye numarası verilmez (10001 gibi yanlış etiket görünmesin).
            val ready = p != null && count != null
            MainMenuScreen(
                levelNumber = if (ready) p!!.levelIndex + 1 else null,
                onContinue = { go(Screen.Game) },
                onHowToPlay = { go(Screen.HowToPlay) },
                onSettings = { go(Screen.Settings) },
                modifier = modifier,
                allDone = ready && count!! > 0 && p!!.levelIndex >= count,
                focusSettings = previous == Screen.Settings,
            )
        }
        Screen.Game -> GameRoute(container, announcer, { go(Screen.Menu) }, modifier)
        Screen.HowToPlay -> HowToPlayScreen({ go(Screen.Menu) }, modifier)
        Screen.Settings -> {
            SettingsScreen(
                soundEnabled = soundSetting ?: true,
                onSoundChange = { scope.launch { container.settingsRepository.setSoundEnabled(it) } },
                onResetConfirmed = {
                    scope.launch {
                        container.progressRepository.save(Progress())
                        announcer.say(Announcements.progressReset())
                    }
                },
                onBack = { go(Screen.Menu) },
                modifier = modifier,
            )
        }
    }
}

/**
 * Oyun ekranı girişte yeni bir GameViewModel kurar, çıkışta temizler; böylece "İlerlemeyi sıfırla"
 * eski bir ViewModel'in durumuyla çakışmaz. (Etkinlik yapılandırma değişiminde yeniden
 * yaratılmaz, bkz. manifest configChanges.)
 */
@Composable
private fun GameRoute(container: AppContainer, announcer: Announcer, onExit: () -> Unit, modifier: Modifier) {
    val store = remember { ViewModelStore() }
    val viewModel = remember(store) {
        ViewModelProvider(store, container.gameViewModelFactory())[GameViewModel::class.java]
    }
    DisposableEffect(store) { onDispose { store.clear() } }
    val feedback = remember(viewModel, announcer) {
        CompositeFeedbackSink(
            AnnouncingFeedbackSink(announcer, { viewModel.state.value }, { SystemClock.elapsedRealtime() }),
            SoundFeedbackSink(container.soundManager) {
                (viewModel.state.value as? GameUiState.Ready)?.tiles?.size ?: DEFAULT_TILE_COUNT
            },
        )
    }
    GameScreen(viewModel, feedback, onExit, modifier)
}

private const val DEFAULT_TILE_COUNT = 7
