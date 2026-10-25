package com.kelimeoyunu.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.MinLetterHeight

/** Seviye sonu: sonuç özeti ve "Sonraki seviye". Odak otomatik olarak düğmeye gelir. */
@Composable
fun LevelCompleteContent(state: GameUiState.Ready, onNextLevel: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            Announcements.levelCompleted(state.levelNumber),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            "Tüm kelimeleri buldun. Toplam jeton: ${state.coins}. " +
                "Bonus sayacı: ${state.bonusMeter} / ${state.bonusTarget}.",
            style = MaterialTheme.typography.bodyLarge,
        )
        A11yButton(
            "Sonraki seviye", "Sonraki seviye", onNextLevel,
            Modifier.fillMaxWidth().initialFocus(), minHeight = MinLetterHeight,
        )
        Text("Bu seviyenin kelimeleri", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        WordList(state.wordRows)
    }
}

/** 10.000. seviye de bitti: tebrik ve ana menüye dönüş. */
@Composable
fun AllDoneContent(onMenu: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tebrikler!", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        Text("Tüm seviyeleri tamamladın.", style = MaterialTheme.typography.bodyLarge)
        A11yButton(
            "Ana menüye dön", "Ana menüye dön", onMenu,
            Modifier.fillMaxWidth().initialFocus(), minHeight = MinLetterHeight,
        )
    }
}
