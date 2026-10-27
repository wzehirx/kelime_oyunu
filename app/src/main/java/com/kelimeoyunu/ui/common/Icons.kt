package com.kelimeoyunu.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Çekirdek `material-icons-extended` yerine Canvas ile çizilen küçük simge kümesi (24 birimlik tuval). */
enum class IconKind { Lock, Check, Play, Coin, Compass, Globe, Book, Sliders, Menu }

/** Süs simgesi: ekran okuyucudan gizlidir; anlam her zaman yanındaki metinde ya da düğüm etiketindedir. */
@Composable
fun KIcon(kind: IconKind, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size).clearAndSetSemantics {}) {
        val s = this.size.minDimension / 24f
        drawIcon(kind, tint, s)
    }
}

private fun DrawScope.line(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float, s: Float) =
    drawLine(c, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), strokeWidth = w * s, cap = StrokeCap.Round)

private fun DrawScope.drawIcon(kind: IconKind, c: Color, s: Float) {
    val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
        IconKind.Lock -> {
            drawArc(c, 180f, 180f, false, Offset(7f * s, 3f * s), Size(10f * s, 10f * s), style = stroke)
            line(c, 7f, 8f, 7f, 11f, 2f, s)
            line(c, 17f, 8f, 17f, 11f, 2f, s)
            drawRoundRect(c, Offset(4f * s, 10f * s), Size(16f * s, 11f * s), androidx.compose.ui.geometry.CornerRadius(2.5f * s))
        }
        IconKind.Check -> {
            val p = Path().apply { moveTo(4.5f * s, 12.5f * s); lineTo(9.5f * s, 17.5f * s); lineTo(19.5f * s, 6.5f * s) }
            drawPath(p, c, style = Stroke(width = 2.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        IconKind.Play -> {
            val p = Path().apply { moveTo(8f * s, 5f * s); lineTo(19f * s, 12f * s); lineTo(8f * s, 19f * s); close() }
            drawPath(p, c)
        }
        IconKind.Coin -> {
            drawCircle(c, 9f * s, Offset(12f * s, 12f * s), style = stroke)
            drawCircle(c, 3.2f * s, Offset(12f * s, 12f * s))
        }
        IconKind.Compass -> {
            drawCircle(c, 9.5f * s, Offset(12f * s, 12f * s), style = Stroke(width = 1.8f * s))
            val north = Path().apply { moveTo(12f * s, 4.5f * s); lineTo(15.2f * s, 12f * s); lineTo(8.8f * s, 12f * s); close() }
            drawPath(north, c)
            val south = Path().apply { moveTo(12f * s, 19.5f * s); lineTo(15.2f * s, 12f * s); lineTo(8.8f * s, 12f * s); close() }
            drawPath(south, c, style = Stroke(width = 1.5f * s, join = StrokeJoin.Round))
        }
        IconKind.Globe -> {
            drawCircle(c, 9f * s, Offset(12f * s, 12f * s), style = stroke)
            drawOval(c, Offset(8f * s, 3f * s), Size(8f * s, 18f * s), style = Stroke(width = 1.6f * s))
            line(c, 3f, 12f, 21f, 12f, 1.6f, s)
        }
        IconKind.Book -> {
            drawRoundRect(
                c, Offset(4.5f * s, 3.5f * s), Size(15f * s, 17f * s),
                androidx.compose.ui.geometry.CornerRadius(2f * s), style = stroke,
            )
            line(c, 8.5f, 3.5f, 8.5f, 20.5f, 2f, s)
            line(c, 12f, 9f, 16.5f, 9f, 1.8f, s)
            line(c, 12f, 13f, 16.5f, 13f, 1.8f, s)
        }
        IconKind.Sliders -> {
            line(c, 4f, 7f, 20f, 7f, 2f, s); line(c, 4f, 12f, 20f, 12f, 2f, s); line(c, 4f, 17f, 20f, 17f, 2f, s)
            drawCircle(c, 2.6f * s, Offset(8f * s, 7f * s))
            drawCircle(c, 2.6f * s, Offset(16f * s, 12f * s))
            drawCircle(c, 2.6f * s, Offset(10f * s, 17f * s))
        }
        IconKind.Menu -> {
            line(c, 4f, 7f, 20f, 7f, 2.4f, s); line(c, 4f, 12f, 20f, 12f, 2.4f, s); line(c, 4f, 17f, 20f, 17f, 2.4f, s)
        }
    }
}
