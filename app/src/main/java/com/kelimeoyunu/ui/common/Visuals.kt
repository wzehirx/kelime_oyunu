package com.kelimeoyunu.ui.common

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * Görünür odak göstergesi: odaktayken 3 dp birincil dış halka, 2 dp aralıkla. Halka bileşenin dışına çizilir
 * (düzeni değiştirmez); köşe yarıçapı [cornerRadius] kadardır (bileşen yarı yüksekliğini aşamaz).
 */
fun Modifier.focusRing(interactionSource: InteractionSource, cornerRadius: Dp): Modifier = composed {
    val focused by interactionSource.collectIsFocusedAsState()
    val color = kelimeColors.primary
    if (!focused) {
        this
    } else {
        drawWithContent {
            drawContent()
            val gap = 2.dp.toPx()
            val stroke = 3.dp.toPx()
            val grow = gap + stroke / 2
            val r = minOf(cornerRadius.toPx(), size.minDimension / 2) + grow
            drawRoundRect(
                color = color,
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + 2 * grow, size.height + 2 * grow),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(stroke),
            )
        }
    }
}

/**
 * Yazı ölçeğini en çok [max] ile sınırlar. Sabit boyutlu görsellerin (jeton, damga, ızgara hücresi, logo karosu)
 * içindeki harfler büyük yazıda taşmasın diye; kaydırılabilir metinler bundan etkilenmez.
 */
@Composable
fun CappedFontScale(max: Float = 1.3f, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    val capped = if (d.fontScale > max) Density(d.density, max) else d
    CompositionLocalProvider(LocalDensity provides capped, content = content)
}
