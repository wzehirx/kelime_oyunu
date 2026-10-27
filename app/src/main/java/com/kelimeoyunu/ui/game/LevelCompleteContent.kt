package com.kelimeoyunu.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.LocalMotion
import com.kelimeoyunu.ui.common.Stamp
import com.kelimeoyunu.ui.common.placeInitials
import com.kelimeoyunu.ui.theme.Elevation
import com.kelimeoyunu.ui.theme.Radius
import com.kelimeoyunu.ui.theme.Space
import com.kelimeoyunu.ui.theme.Touch
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * Seviye sonu: kutlama kartı (başlık + ülke bitişinde damga), "Sonraki seviye", kazanımlar ve bu seviyenin kelimeleri.
 * [headingFirst] (biten seviye kayıttan yeniden açıldığında: hiçbir duyuru gelmez) başlangıç odağı
 * "Seviye N tamamlandı" başlığına gider, ardından düğme gelir. Seviye oyun içinde bittiğinde duyuru aynı cümleyi zaten
 * söyler; çift okuma olmasın diye odak doğrudan düğmeye gider. Konfeti ve damga yalnızca görseldir; yalnızca seviye
 * oyun içinde bittiğinde ([headingFirst] false) oynar.
 */
@Composable
fun LevelCompleteContent(
    state: GameUiState.Ready,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier,
    headingFirst: Boolean = false,
) {
    val k = kelimeColors
    val place = state.place
    val endsPlace = place != null && place.levelInPlace == place.placeLevelCount
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.panel), color = k.surface,
                shadowElevation = Elevation.panel, border = BorderStroke(1.dp, k.line),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(Space.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    if (endsPlace && place != null) {
                        PressedStamp(placeInitials(place.place.ad), k.regionInk(place.place.bolge))
                    }
                    Text(
                        Announcements.levelCompleted(state.levelNumber),
                        Modifier.then(if (headingFirst) Modifier.initialFocus() else Modifier).semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium,
                        color = k.ink, textAlign = TextAlign.Center,
                    )
                }
            }
            A11yButton(
                "Sonraki seviye", "Sonraki seviye", onNextLevel,
                Modifier.fillMaxWidth().then(if (headingFirst) Modifier else Modifier.initialFocus()),
                minHeight = Touch.primary, pill = true,
            )
            Text(
                "Tüm kelimeleri buldun. Toplam jeton: ${state.coins}. " +
                    "Bonus sayacı: ${state.bonusMeter} / ${state.bonusTarget}.",
                style = MaterialTheme.typography.bodyLarge, color = k.ink,
            )
            Text(
                "Bu seviyenin kelimeleri", Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge, color = k.ink,
            )
            WordList(state.wordRows)
        }
        if (!headingFirst) Confetti(Modifier.fillMaxSize())
    }
}

/** Ülke bitişi damgası: ekrana "basılır" (ölçek 1,6 -> 1,0, hafif zıplama, 450 ms). Süs; hareket kapalıysa anlık. */
@Composable
private fun PressedStamp(initials: String, ink: Color) {
    val motion = LocalMotion.current
    val scale = remember { Animatable(if (motion.enabled) 1.6f else 1f) }
    LaunchedEffect(Unit) {
        if (motion.enabled) scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
    Stamp(initials, ink, Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }, size = 96.dp)
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
            color = kelimeColors.ink,
        )
        A11yButton(
            "Ana menüye dön", "Ana menüye dön", onMenu,
            Modifier.fillMaxWidth().then(if (headingFirst) Modifier else Modifier.initialFocus()),
            minHeight = Touch.primary, pill = true,
        )
    }
}
