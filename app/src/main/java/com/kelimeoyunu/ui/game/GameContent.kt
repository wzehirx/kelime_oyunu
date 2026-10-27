package com.kelimeoyunu.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
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
    BoxWithConstraints(modifier.fillMaxSize().semantics { isTraversalGroup = true }) {
        val twoColumns = maxWidth >= 600.dp
        // Yüksek ekranda (>= 720 dp, normal yazı) eylem alanı (kelime hapı + çark + eylemler) alta sabitlenir; üst kısım
        // (başlık, ızgara, liste) kaydırılır. Düşük ekranda ya da büyük yazıda her şey birlikte kaydırılır.
        val pinned = !twoColumns && maxHeight >= 720.dp && LocalDensity.current.fontScale <= 1.15f
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(48.dp)) // "Menü" düğmesinin yeri (üstte yüzer)
            if (pinned) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GameHeader(state)
                    CrosswordGrid(state.grid, state.levelNumber)
                    WordList(state.wordRows)
                }
                Column(Modifier.padding(top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrentWordField(state.currentWord, bonusTick = effects.bonusTick)
                    PlayPanel(
                        state.tiles, state.selection, callbacks, submitDimmed = state.currentWord.isEmpty(),
                        maxDiameter = 250.dp, effects = effects,
                    )
                }
            } else {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GameHeader(state)
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
        // En son yazılır: ağaç sırasında son, açık traversalIndex ile de en sonda okunur.
        MenuChip(
            callbacks.onMenu,
            Modifier.align(Alignment.TopStart).padding(start = 12.dp).semantics { traversalIndex = 1000f },
        )
    }
}
