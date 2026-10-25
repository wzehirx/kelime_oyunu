package com.kelimeoyunu.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.ui.common.A11yButton

/** ViewModel'e bağlı oyun ekranı: durumu çizer, olayları [feedback]'e (duyuru, ileride ses) yönlendirir. */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    feedback: FeedbackSink,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel, feedback) { viewModel.events.collect { feedback.onEvent(it) } }
    val callbacks = remember(viewModel) { GameCallbacks.of(viewModel) }
    when (val s = state) {
        GameUiState.Loading -> MessageScreen("Yükleniyor", null, null, modifier)
        is GameUiState.Error -> MessageScreen("Hata: ${s.message}", "Ana menü", onExitToMenu, modifier)
        GameUiState.AllDone -> AllDoneContent(onExitToMenu, modifier)
        is GameUiState.Ready -> GameContent(s, callbacks, modifier)
    }
}

@Composable
private fun MessageScreen(message: String, buttonLabel: String?, onButton: (() -> Unit)?, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            message,
            Modifier.initialFocus().semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        if (buttonLabel != null && onButton != null) {
            A11yButton(buttonLabel, buttonLabel, onButton, Modifier.fillMaxWidth())
        }
    }
}
