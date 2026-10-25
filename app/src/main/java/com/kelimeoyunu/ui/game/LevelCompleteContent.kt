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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.MinLetterHeight

/**
 * Seviye sonu: sonuç özeti ve "Sonraki seviye". [headingFirst] (biten seviye kayıttan yeniden açıldığında:
 * hiçbir duyuru gelmez) başlangıç odağı "Seviye N tamamlandı" başlığına gider, ardından düğme gelir.
 * Seviye oyun içinde bittiğinde duyuru aynı cümleyi zaten söyler; çift okuma olmasın diye odak
 * doğrudan düğmeye gider.
 */
@Composable
fun LevelCompleteContent(
    state: GameUiState.Ready,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier,
    headingFirst: Boolean = false,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            Announcements.levelCompleted(state.levelNumber),
            Modifier.then(if (headingFirst) Modifier.initialFocus() else Modifier).semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        A11yButton(
            "Sonraki seviye", "Sonraki seviye", onNextLevel,
            Modifier.fillMaxWidth().then(if (headingFirst) Modifier else Modifier.initialFocus()),
            minHeight = MinLetterHeight,
        )
        Text(
            "Tüm kelimeleri buldun. Toplam jeton: ${state.coins}. " +
                "Bonus sayacı: ${state.bonusMeter} / ${state.bonusTarget}.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("Bu seviyenin kelimeleri", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        WordList(state.wordRows)
    }
}

/**
 * 10.000. seviye de bitti: tebrik ve ana menüye dönüş. [headingFirst] (menüden girişte, duyuru yok):
 * ilk odak başlıkta, anlamlı açıklamayla ("Tüm N seviyeyi tamamladın"); ardından "Ana menüye dön".
 */
@Composable
fun AllDoneContent(
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    levelCount: Int? = null,
    headingFirst: Boolean = false,
) {
    val description = if (levelCount != null && levelCount > 0) {
        "Tebrikler! Tüm seviyeleri tamamladın. Toplam $levelCount seviye."
    } else {
        "Tebrikler! Tüm seviyeleri tamamladın."
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Tüm seviyeleri tamamladın",
            Modifier.then(if (headingFirst) Modifier.initialFocus() else Modifier)
                .semantics { heading(); contentDescription = description },
            style = MaterialTheme.typography.headlineMedium,
        )
        A11yButton(
            "Ana menüye dön", "Ana menüye dön", onMenu,
            Modifier.fillMaxWidth().then(if (headingFirst) Modifier else Modifier.initialFocus()),
            minHeight = MinLetterHeight,
        )
    }
}
