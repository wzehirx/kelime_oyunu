package com.kelimeoyunu.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.initialFocus
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.kelimeoyunu.AppContainer
import com.kelimeoyunu.a11y.AnnouncingFeedbackSink
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.Announcer
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.domain.WorldTour
import com.kelimeoyunu.ui.common.HapticFeedbackSink
import com.kelimeoyunu.ui.common.ProvideMotion
import com.kelimeoyunu.ui.common.ViewHaptics
import com.kelimeoyunu.ui.game.CompositeFeedbackSink
import com.kelimeoyunu.ui.game.GameScreen
import com.kelimeoyunu.ui.game.GameUiState
import com.kelimeoyunu.ui.game.GameViewModel
import com.kelimeoyunu.ui.game.SoundFeedbackSink
import com.kelimeoyunu.ui.menu.HowToPlayScreen
import com.kelimeoyunu.ui.menu.MainMenuScreen
import com.kelimeoyunu.ui.settings.SettingsScreen
import com.kelimeoyunu.ui.world.WorldTourScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Menu, Game, HowToPlay, Settings, World }

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
    // Titreşim ve animasyon ayarları varsayılan açıktır; değer gelene kadar da açık varsayılır.
    val vibrationSetting by container.settingsRepository.vibrationEnabled.collectAsState(initial = true)
    val animationsSetting by container.settingsRepository.animationsEnabled.collectAsState(initial = true)

    fun go(next: Screen) {
        container.soundManager.play(Sfx.NAV)
        previous = screen
        screen = next
    }
    BackHandler(enabled = screen != Screen.Menu) { go(Screen.Menu) }

    ProvideMotion(animationsSetting) {
    when (screen) {
        Screen.Menu -> {
            val progress by container.progressRepository.progress.collectAsState(initial = null)
            val levels by produceState<Pair<Int, WorldTour?>?>(null, container) {
                value = withContext(Dispatchers.Default) { container.levelRepository.count to container.worldTour }
            }
            val p = progress
            val count = levels?.first
            val tour = levels?.second
            // İkisi de okunana kadar seviye numarası verilmez (10001 gibi yanlış etiket görünmesin).
            val ready = p != null && count != null
            MainMenuScreen(
                levelNumber = if (ready) p!!.levelIndex + 1 else null,
                onContinue = { go(Screen.Game) },
                place = if (ready) tour?.positionOf(p!!.levelIndex) else null,
                onWorldTour = if (tour != null) ({ go(Screen.World) }) else null,
                focusWorld = previous == Screen.World,
                onHowToPlay = { go(Screen.HowToPlay) },
                onSettings = { go(Screen.Settings) },
                modifier = modifier,
                allDone = ready && count!! > 0 && p!!.levelIndex >= count,
                focusSettings = previous == Screen.Settings,
                coins = if (ready) p!!.coins else null,
                totalLevels = if (ready) count else null,
                worldCompleted = if (ready && tour != null) {
                    // Dünya turu ekranıyla aynı sayım: biten ama "Sonraki seviye"ye basılmamış seviye ilerlemeye sayılır.
                    val done = container.levelRepository.isLevelComplete(p!!.levelIndex, p.found)
                    tour.completedPlaces(p.levelIndex + if (done) 1 else 0)
                } else null,
                worldTotal = if (ready) tour?.placeCount else null,
            )
        }
        Screen.Game -> GameRoute(container, announcer, { go(Screen.Menu) }, modifier, vibrationSetting)
        Screen.World -> WorldRoute(container, announcer, { go(Screen.Menu) }, modifier)
        Screen.HowToPlay -> {
            // Tur kapalıysa (places.json yok/bozuk) Dünya turu anlatımı gösterilmez; bilinene kadar da gösterilmez.
            val tourOn by produceState(false, container) { value = withContext(Dispatchers.Default) { container.worldTour != null } }
            HowToPlayScreen({ go(Screen.Menu) }, modifier, worldTourEnabled = tourOn)
        }
        Screen.Settings -> {
            SettingsScreen(
                soundEnabled = soundSetting ?: true,
                onSoundChange = { scope.launch { container.settingsRepository.setSoundEnabled(it) } },
                vibrationEnabled = vibrationSetting,
                onVibrationChange = { scope.launch { container.settingsRepository.setVibrationEnabled(it) } },
                animationsEnabled = animationsSetting,
                onAnimationsChange = { scope.launch { container.settingsRepository.setAnimationsEnabled(it) } },
                onResetConfirmed = {
                    scope.launch {
                        container.progressRepository.reset()
                        announcer.say(Announcements.progressReset())
                    }
                },
                onBack = { go(Screen.Menu) },
                modifier = modifier,
            )
        }
    }
    }
}

/**
 * Oyun ekranı girişte yeni bir GameViewModel kurar, çıkışta temizler; böylece "İlerlemeyi sıfırla"
 * eski bir ViewModel'in durumuyla çakışmaz. (Etkinlik yapılandırma değişiminde yeniden
 * yaratılmaz, bkz. manifest configChanges.)
 */
@Composable
private fun GameRoute(
    container: AppContainer, announcer: Announcer, onExit: () -> Unit, modifier: Modifier, vibrationEnabled: Boolean,
) {
    val view = LocalView.current
    val vibration = rememberUpdatedState(vibrationEnabled)
    val store = remember { ViewModelStore() }
    val viewModel = remember(store) {
        ViewModelProvider(store, container.gameViewModelFactory())[GameViewModel::class.java]
    }
    DisposableEffect(store) { onDispose { store.clear() } }
    val feedback = remember(viewModel, announcer, view) {
        CompositeFeedbackSink(
            AnnouncingFeedbackSink(
                announcer, { viewModel.state.value }, { SystemClock.elapsedRealtime() },
                soundOn = { container.soundManager.enabled },
            ),
            SoundFeedbackSink(container.soundManager),
            HapticFeedbackSink(ViewHaptics(view) { vibration.value }),
        )
    }
    GameScreen(viewModel, feedback, onExit, modifier)
}

/** Dünya turu listesi: tur ve ilerleme okunana kadar "Yükleniyor" başlığı gösterir. */
@Composable
private fun WorldRoute(container: AppContainer, announcer: Announcer, onBack: () -> Unit, modifier: Modifier) {
    val tour by produceState<WorldTour?>(null, container) { value = withContext(Dispatchers.Default) { container.worldTour } }
    val progress by container.progressRepository.progress.collectAsState(initial = null)
    val t = tour
    val p = progress
    if (t == null || p == null) {
        Text("Yükleniyor", modifier.padding(16.dp).initialFocus().semantics { heading() })
    } else {
        val done = container.levelRepository.isLevelComplete(p.levelIndex, p.found)
        WorldTourScreen(t, p.levelIndex, { announcer.say(it) }, onBack, modifier, savedLevelDone = done)
    }
}
