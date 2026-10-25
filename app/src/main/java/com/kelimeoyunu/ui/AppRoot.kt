package com.kelimeoyunu.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.kelimeoyunu.data.Progress
import com.kelimeoyunu.ui.game.GameScreen
import com.kelimeoyunu.ui.game.GameViewModel
import com.kelimeoyunu.ui.menu.HowToPlayScreen
import com.kelimeoyunu.ui.menu.MainMenuScreen
import com.kelimeoyunu.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

enum class Screen { Menu, Game, HowToPlay, Settings }

/** Basit durum tabanlı gezinme. Menü dışındaki ekranlardan geri tuşu menüye döner. */
@Composable
fun AppRoot(container: AppContainer, modifier: Modifier = Modifier) {
    var screen by rememberSaveable { mutableStateOf(Screen.Menu) }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val announcer = remember { Announcer({ view.announceForAccessibility(it) }, scope) }
    BackHandler(enabled = screen != Screen.Menu) { screen = Screen.Menu }

    when (screen) {
        Screen.Menu -> {
            val progress by container.progressRepository.progress.collectAsState(initial = null)
            MainMenuScreen(
                levelNumber = progress?.let { it.levelIndex + 1 },
                onContinue = { screen = Screen.Game },
                onHowToPlay = { screen = Screen.HowToPlay },
                onSettings = { screen = Screen.Settings },
                modifier = modifier,
            )
        }
        Screen.Game -> GameRoute(container, announcer, { screen = Screen.Menu }, modifier)
        Screen.HowToPlay -> HowToPlayScreen({ screen = Screen.Menu }, modifier)
        Screen.Settings -> {
            val sound by container.settingsRepository.soundEnabled.collectAsState(initial = true)
            SettingsScreen(
                soundEnabled = sound,
                onSoundChange = { scope.launch { container.settingsRepository.setSoundEnabled(it) } },
                onResetConfirmed = {
                    scope.launch {
                        container.progressRepository.save(Progress())
                        announcer.say(Announcements.progressReset())
                    }
                },
                onBack = { screen = Screen.Menu },
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
        AnnouncingFeedbackSink(announcer, { viewModel.state.value }, { SystemClock.elapsedRealtime() })
    }
    GameScreen(viewModel, feedback, onExit, modifier)
}
