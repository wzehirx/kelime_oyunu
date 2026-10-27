package com.kelimeoyunu.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.ui.common.LocalMotion
import com.kelimeoyunu.ui.theme.Elevation
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.Radius
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * "Oluşturulan kelime: KAL" hapı: seçilen harfler düz başlık metni olarak, boşken soluk "Harf seç" ipucu. Bilerek
 * liveRegion DEĞİL: değişiklikleri Announcer okur, canlı bölge de okusaydı her harf iki kez duyulurdu. [bonusTick]
 * artınca "+Bonus" rozeti yukarı süzülüp kaybolur (600 ms; hareket kapalıysa gösterilmez).
 */
@Composable
fun CurrentWordField(word: String, modifier: Modifier = Modifier, bonusTick: Int = 0) {
    val k = kelimeColors
    val motion = LocalMotion.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                .clearAndSetSemantics { contentDescription = Announcements.currentWordDescription(word) },
            color = k.surface,
            border = BorderStroke(2.dp, if (word.isEmpty()) k.line else k.primary),
            shape = RoundedCornerShape(Radius.pill),
            shadowElevation = Elevation.panel,
        ) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                if (word.isEmpty()) {
                    Text("Harf seç", style = KelimeType.small, color = k.inkMuted)
                } else {
                    Text(
                        TurkishText.upper(word), style = KelimeType.title, color = k.ink,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Clip,
                    )
                }
            }
        }
        BonusFloat(bonusTick, motion.enabled)
    }
}

/** "+Bonus": yukarı süzülür ve solar (600 ms). Süs; ekran okuyucudan gizli. Yalnızca [tick] değişince çalışır. */
@Composable
private fun BonusFloat(tick: Int, enabled: Boolean) {
    val k = kelimeColors
    val progress = remember { Animatable(1f) }
    var handled by remember { mutableIntStateOf(tick) }
    LaunchedEffect(tick, enabled) {
        if (tick != handled) {
            handled = tick
            if (enabled) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(600, easing = LinearOutSlowInEasing))
            }
        }
    }
    if (progress.value < 1f) {
        Text(
            "+Bonus",
            Modifier.clearAndSetSemantics {}.graphicsLayer {
                translationY = -56.dp.toPx() * progress.value
                alpha = 1f - progress.value
            },
            style = KelimeType.label, color = k.success,
        )
    }
}
