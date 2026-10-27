package com.kelimeoyunu.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp

/**
 * Durumsuz oyun ekranı. TalkBack okuma sırası yukarıdan aşağı: başlık, kelime listesi, oluşturulan kelime, harfler,
 * eylem düğmeleri (Gönder, Sil, Temizle, Karıştır, İpucu, Kelime aç). Kaydırılabilir; büyük yazıda taşmaz.
 * Genişlik >= 600 dp (ya da yatay) iki sütun: solda ızgara + kelime listesi, sağda kelime + çark + eylemler; okuma
 * sırası DEĞİŞMEZ (sütunlar açık traversalIndex ile sıralanır). Izgara yalnızca görseldir (ekran okuyucudan gizli).
 */
@Composable
fun GameContent(
    state: GameUiState.Ready,
    callbacks: GameCallbacks,
    modifier: Modifier = Modifier,
    levelCompleteHeadingFirst: Boolean = false,
    effects: GameEffects = remember { GameEffects() },
) {
    if (state.levelComplete) {
        LevelCompleteContent(state, callbacks.onNextLevel, modifier, levelCompleteHeadingFirst)
        return
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val twoColumns = maxWidth >= 600.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GameHeader(state, onMenu = callbacks.onMenu)
            if (twoColumns) {
                Row(Modifier.fillMaxWidth().semantics { isTraversalGroup = true }, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(
                        Modifier.weight(1f).semantics { isTraversalGroup = true; traversalIndex = 0f },
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CrosswordGrid(state.grid, state.levelNumber)
                        WordList(state.wordRows)
                    }
                    Column(
                        Modifier.weight(1f).semantics { isTraversalGroup = true; traversalIndex = 1f },
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CurrentWordField(state.currentWord, bonusTick = effects.bonusTick)
                        PlayPanel(state.tiles, state.selection, callbacks, submitDimmed = state.currentWord.isEmpty(), effects = effects)
                    }
                }
            } else {
                CrosswordGrid(state.grid, state.levelNumber)
                WordList(state.wordRows)
                CurrentWordField(state.currentWord, bonusTick = effects.bonusTick)
                PlayPanel(state.tiles, state.selection, callbacks, submitDimmed = state.currentWord.isEmpty(), effects = effects)
            }
        }
    }
}
