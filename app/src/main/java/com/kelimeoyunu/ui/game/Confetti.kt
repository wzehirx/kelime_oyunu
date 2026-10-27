package com.kelimeoyunu.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.kelimeoyunu.ui.common.LocalMotion
import com.kelimeoyunu.ui.theme.kelimeColors
import kotlin.random.Random

private class Piece(val x: Float, val delay: Float, val speed: Float, val spin: Float, val color: Int, val w: Float, val h: Float)

/**
 * Seviye bitişi konfetisi: 40 parça, Canvas, en çok 1,5 sn, yerçekimiyle doğrusal düşüş. Hareket kapalıysa hiç
 * çizilmez. Süstür; ekran okuyucudan gizli ve dokunmayı engellemez.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier, seed: Int = 7) {
    val motion = LocalMotion.current
    if (!motion.enabled) return
    val colors = kelimeColors.regionInks
    val pieces = remember(seed) {
        val r = Random(seed)
        List(40) { Piece(r.nextFloat(), r.nextFloat() * 0.35f, 0.65f + r.nextFloat() * 0.35f, r.nextFloat() * 720f - 360f, r.nextInt(colors.size), 6f + r.nextFloat() * 6f, 10f + r.nextFloat() * 8f) }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1500, easing = LinearEasing)) }
    if (t.value >= 1f) return
    Canvas(modifier.fillMaxSize().clearAndSetSemantics {}) {
        pieces.forEach { p ->
            val local = ((t.value - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val x = p.x * size.width
            val y = -20f + local * local * p.speed * (size.height + 40f) * 0.4f + local * p.speed * (size.height + 40f) * 0.6f
            rotate(p.spin * local, Offset(x, y)) {
                drawRect(
                    colors[p.color].copy(alpha = 1f - local * 0.4f), Offset(x - p.w / 2, y - p.h / 2), Size(p.w, p.h),
                )
            }
        }
    }
}
