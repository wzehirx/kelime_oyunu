package com.kelimeoyunu.ui.menu

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
import com.kelimeoyunu.domain.PlacePosition
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.MinLetterHeight

/**
 * Ana menü. Başlangıç odağı "Devam et" düğmesindedir ([focusSettings] ise ayarlardan dönüldüğü için
 * "Ayarlar"da, [focusWorld] ise Dünya turu ekranından dönüldüğü için "Dünya turu"nda). [levelNumber] null ise
 * ilerleme henüz okunmadı; [allDone] ise tüm seviyeler bitmiştir. [place] varsa "Devam et" ülkeyi söyler;
 * [onWorldTour] null ise (tur kapalı) "Dünya turu" düğmesi gösterilmez.
 */
@Composable
fun MainMenuScreen(
    levelNumber: Int?,
    onContinue: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    allDone: Boolean = false,
    focusSettings: Boolean = false,
    place: PlacePosition? = null,
    onWorldTour: (() -> Unit)? = null,
    focusWorld: Boolean = false,
) {
    val continueLabel = when {
        allDone -> "Devam et, tüm seviyeler tamamlandı"
        levelNumber == null -> "Devam et"
        place != null -> Announcements.continueLabel(place)
        else -> "Devam et, Seviye $levelNumber"
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Kelime Oyunu", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
        A11yButton(
            continueLabel, continueLabel, onContinue,
            Modifier.fillMaxWidth().then(if (focusSettings || focusWorld) Modifier else Modifier.initialFocus()),
            minHeight = MinLetterHeight,
        )
        if (onWorldTour != null) {
            A11yButton(
                "Dünya turu", "Dünya turu", onWorldTour,
                Modifier.fillMaxWidth().then(if (focusWorld) Modifier.initialFocus() else Modifier),
                minHeight = MinLetterHeight,
            )
        }
        A11yButton("Nasıl oynanır", "Nasıl oynanır", onHowToPlay, Modifier.fillMaxWidth(), minHeight = MinLetterHeight)
        A11yButton(
            "Ayarlar", "Ayarlar", onSettings,
            Modifier.fillMaxWidth().then(if (focusSettings) Modifier.initialFocus() else Modifier),
            minHeight = MinLetterHeight,
        )
    }
}
