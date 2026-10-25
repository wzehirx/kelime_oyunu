package com.kelimeoyunu.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Durumsuz oyun ekranı. TalkBack okuma sırası yukarıdan aşağı: başlık, kelime listesi,
 * oluşturulan kelime, harfler, eylem düğmeleri. Kaydırılabilir; büyük yazıda taşmaz.
 */
@Composable
fun GameContent(state: GameUiState.Ready, callbacks: GameCallbacks, modifier: Modifier = Modifier) {
    if (state.levelComplete) {
        LevelCompleteContent(state, callbacks.onNextLevel, modifier)
        return
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GameHeader(state)
        WordList(state.wordRows)
        CurrentWordField(state.currentWord)
        LetterPad(state.tiles, callbacks.onLetter)
        ActionBar(callbacks)
    }
}
