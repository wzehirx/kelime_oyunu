package com.kelimeoyunu.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.ButtonKind
import com.kelimeoyunu.ui.common.MinLetterHeight
import com.kelimeoyunu.ui.theme.kelimeColors

/** ViewModel'e bağlı oyun ekranı: durumu çizer, olayları [feedback]'e (duyuru, ileride ses) yönlendirir. */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    feedback: FeedbackSink,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val effects = remember(viewModel) { GameEffects() }
    LaunchedEffect(viewModel, feedback) { viewModel.events.collect { feedback.onEvent(it); effects.onEvent(it) } }
    val callbacks = remember(viewModel, onExitToMenu) { GameCallbacks.of(viewModel).copy(onMenu = onExitToMenu) }
    // Ekrana ilk girişte son durumda mıyız? Öyleyse hiçbir olay duyurusu gelmez; bağlamı odak taşır.
    val enteredAtEnd = remember { arrayOfNulls<Boolean>(1) }
    when (val s = state) {
        GameUiState.Loading -> MessageScreen("Yükleniyor", null, null, modifier, loading = true)
        is GameUiState.Error -> MessageScreen("Hata: ${s.message}", "Ana menü", onExitToMenu, modifier, retry = viewModel::retry)
        GameUiState.AllDone -> {
            if (enteredAtEnd[0] == null) enteredAtEnd[0] = true
            AllDoneContent(onExitToMenu, modifier, levelCount = viewModel.levelCount, headingFirst = enteredAtEnd[0] == true)
        }
        is GameUiState.Ready -> {
            if (enteredAtEnd[0] == null) enteredAtEnd[0] = false
            // Seviye başına: bu seviye ekrana zaten bitmiş olarak mı geldi (kayıttan yeniden açıldı)?
            val reopenedComplete = remember(s.levelNumber) { s.levelComplete }
            GameContent(s, callbacks, modifier, levelCompleteHeadingFirst = reopenedComplete, effects = effects)
        }
    }
}

@Composable
private fun MessageScreen(
    message: String, buttonLabel: String?, onButton: (() -> Unit)?, modifier: Modifier,
    loading: Boolean = false, retry: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            message,
            Modifier.initialFocus().semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = kelimeColors.ink,
        )
        // Yükleme göstergesi süstür: durumu başlık ("Yükleniyor") zaten söyler.
        if (loading) CircularProgressIndicator(Modifier.clearAndSetSemantics {}, color = kelimeColors.primary)
        if (retry != null) {
            A11yButton("Yeniden dene", "Yeniden dene", retry, Modifier.fillMaxWidth(), minHeight = MinLetterHeight)
        }
        if (buttonLabel != null && onButton != null) {
            A11yButton(
                buttonLabel, buttonLabel, onButton, Modifier.fillMaxWidth(), minHeight = MinLetterHeight,
                kind = if (retry != null) ButtonKind.Secondary else ButtonKind.Primary,
            )
        }
    }
}
