package com.kelimeoyunu.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.initialFocus

/** Seviye, jeton, kalan kelime ve bonus sayacı; TalkBack'te tek başlık düğümü olarak okunur. */
@Composable
fun GameHeader(state: GameUiState.Ready, modifier: Modifier = Modifier) {
    val description = Announcements.headerDescription(
        state.levelNumber, state.coins, state.bonusMeter, state.bonusTarget, state.remainingWords,
    )
    Column(
        modifier.fillMaxWidth().initialFocus().semantics(mergeDescendants = true) {
            heading()
            contentDescription = description
        },
    ) {
        Text("Seviye ${state.levelNumber}", Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Jeton: ${state.coins}  |  Kalan kelime: ${state.remainingWords}  |  Bonus: ${state.bonusMeter} / ${state.bonusTarget}",
            Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
