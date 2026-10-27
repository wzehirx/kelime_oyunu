package com.kelimeoyunu.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.ui.common.LocalMotion
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.Radius
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * Gerçek çapraz bulmaca ızgarası: kareler, bulunan harfler (fildişi), ipucuyla açılanlar (mavi ton). Yalnızca görseldir;
 * ekran okuyucudan GİZLİDİR (`clearAndSetSemantics`), bilgiyi kelime listesi taşır. Izgara dışı kutular çizilmez.
 * Hücre `min(36 dp, genişlik / sütun)`; hiçbir zaman yatay kaydırma olmaz, dar alanda hücre küçülür.
 * Yeni açılan harfler 60 ms arayla sırayla belirir (hareket kapalıysa anlık).
 */
@Composable
fun CrosswordGrid(grid: GridUi?, levelKey: Int, modifier: Modifier = Modifier) {
    if (grid == null || grid.rows == 0 || grid.cols == 0) return
    val motion = LocalMotion.current
    // Önceki kompozisyonda dolu olan kutular: yalnızca ondan sonra dolanlar animasyonla belirir.
    val previous = remember(levelKey) { arrayOfNulls<Set<Pair<Int, Int>>>(1) }
    val visible = grid.cells.filter { it.letter != null }.map { it.row to it.col }.toSet()
    val fresh: List<Pair<Int, Int>> = previous[0]?.let { prev -> (visible - prev).sortedWith(compareBy({ it.first }, { it.second })) }.orEmpty()
    SideEffect { previous[0] = visible }

    androidx.compose.runtime.key(levelKey) { BoxWithConstraints(modifier.fillMaxWidth().clearAndSetSemantics {}, contentAlignment = Alignment.TopCenter) {
        val cell: Dp = minOf(36.dp, maxWidth / grid.cols)
        Box(Modifier.size(cell * grid.cols, cell * grid.rows)) {
            grid.cells.forEach { c ->
                val order = fresh.indexOf(c.row to c.col)
                GridCell(
                    c, cell,
                    startDelayMs = if (order >= 0 && motion.enabled) order * 60 else 0,
                    Modifier.offset(cell * c.col, cell * c.row),
                )
            }
        }
    } }
}

@Composable
private fun GridCell(c: GridCellUi, cell: Dp, startDelayMs: Int, modifier: Modifier) {
    val k = kelimeColors
    val motion = LocalMotion.current
    val open = c.letter != null
    val appear = remember { Animatable(if (open) 1f else 0f) }
    val delayMs by rememberUpdatedState(startDelayMs)
    LaunchedEffect(open) {
        if (open) {
            if (appear.value < 1f) {
                if (delayMs > 0) delay(delayMs.toLong())
                appear.animateTo(1f, tween(motion.ms(160), easing = FastOutSlowInEasing))
            }
        } else {
            appear.snapTo(0f)
        }
    }
    val pad = 1.5.dp
    val shape = RoundedCornerShape(Radius.cell)
    Box(modifier.size(cell).padding(pad)) {
        val fill = when {
            !open -> k.surface
            c.hinted -> k.primaryTint
            else -> k.tile
        }
        val ink = when {
            c.hinted -> k.ink
            else -> k.tileInk
        }
        // Kapalı kutu: ince çizgi (color-line) çerçeveli boş; açık kutu: dolgu + harf.
        Box(Modifier.size(cell - pad * 2).background(fill, shape).border(1.dp, if (open) k.tileEdge else k.line, shape))
        if (open) {
            Box(
                Modifier.size(cell - pad * 2)
                    .graphicsLayer {
                        val t = appear.value
                        alpha = t
                        scaleX = 0.8f + 0.2f * t
                        scaleY = 0.8f + 0.2f * t
                    },
                contentAlignment = Alignment.Center,
            ) {
                val textSize = with(LocalDensity.current) { (cell * 0.52f).toSp() }
                Text(
                    TurkishText.upper(c.letter.toString()),
                    style = KelimeType.label.copy(fontSize = textSize, lineHeight = textSize),
                    color = ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}
