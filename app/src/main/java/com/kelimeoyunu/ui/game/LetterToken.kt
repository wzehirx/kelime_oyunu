package com.kelimeoyunu.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.ui.common.CappedFontScale
import com.kelimeoyunu.ui.common.LocalMotion
import com.kelimeoyunu.ui.common.focusRing
import com.kelimeoyunu.ui.theme.Elevation
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * Harf jetonu: fildişi daire; seçiliyse birincil mavi dolgu + beyaz harf + sağ üstte sıra rozeti ([badge], yalnızca
 * görsel). Anlamsal etiket "Harf K" / "Harf K, seçildi" (rozet ve harf metni gizli). Basınca ölçek 0,94 (80 ms).
 * [enabled] false ise %40 opaklıkta ve tıklanamaz.
 */
@Composable
fun LetterToken(
    tile: LetterTile,
    badge: Int?,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val k = kelimeColors
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        tween(if (pressed) motion.ms(80) else motion.ms(120), easing = FastOutSlowInEasing),
        label = "tokenPress",
    )
    val selected = tile.used
    Box(
        modifier.size(size).focusRing(interaction, size / 2)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.4f }
            .clickable(
                interactionSource = interaction, indication = null, enabled = enabled,
                role = Role.Button, onClick = onClick,
            )
            .semantics { contentDescription = Announcements.letterButton(tile.char, tile.used) },
    ) {
        Box(
            Modifier.size(size).shadow(Elevation.token, CircleShape).clip(CircleShape)
                .background(if (selected) k.primary else k.tile)
                .border(if (selected) 0.dp else 1.5.dp, k.tileEdge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            CappedFontScale(1.3f) {
                Text(
                    TurkishText.upper(tile.char.toString()),
                    Modifier.clearAndSetSemantics {},
                    style = KelimeType.tile, color = if (selected) k.onPrimary else k.tileInk,
                    textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
        if (badge != null) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(22.dp)
                    .background(k.tile, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(19.dp).background(k.ink, CircleShape), contentAlignment = Alignment.Center) {
                    CappedFontScale(1f) {
                        Text(
                            badge.toString(), Modifier.clearAndSetSemantics {},
                            style = KelimeType.small.copy(lineHeight = KelimeType.small.fontSize, fontWeight = FontWeight.Bold), color = k.bg,
                            maxLines = 1, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
