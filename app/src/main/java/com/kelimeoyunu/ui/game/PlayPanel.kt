package com.kelimeoyunu.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.domain.GameEconomy
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.ButtonKind
import com.kelimeoyunu.ui.common.IconKind
import com.kelimeoyunu.ui.common.KIcon
import com.kelimeoyunu.ui.common.LocalMotion
import com.kelimeoyunu.ui.common.focusRing
import com.kelimeoyunu.ui.theme.kelimeColors
import kotlin.math.cos
import kotlin.math.sin

/** Çark jetonunun halkadaki (kesirli) yuvası; Karıştır'da yeni yuvaya kayarken kesirli değerler alır. */
private class TokenSlot(val slot: State<Float>)

private enum class Part { Send, Delete, Clear, Shuffle, Hint, Word }

/**
 * Pusula halkası harf çarkı ve eylem düğmeleri. Tek özel yerleşimdir; çocukların YAZILIŞ sırası TalkBack okuma
 * sırasıdır ve eskisiyle aynıdır: harfler (havuz sırası), Gönder, Sil, Temizle, Karıştır, İpucu, Kelime aç.
 * Görsel konum bundan bağımsızdır (Karıştır çarkın ortasında, Sil/Gönder/Temizle yan yana); okuma sırasını
 * geometri değil açık `traversalIndex` belirler.
 *
 * Yerleşim: disk çapı en çok 320 dp; halka üzerinde eşit aralıklı jetonlar (ilk jeton saat 12'de, 360/n derece);
 * altta birinci eylem satırı (Sil, büyük Gönder, Temizle), sonra İpucu ve Kelime aç. Dar ekranda (< 300 dp) ya da
 * büyük yazıda (> 1,15x) Gönder tam genişlik olur, Sil/Temizle altına iner; çok büyük yazıda (> 1,5x) ya da dar
 * ekranda ipucu düğmeleri tek sütundur. Çark ve jetonlar sabit dp'dir (yazıyla büyümez).
 */
@Composable
fun PlayPanel(
    tiles: List<LetterTile>,
    selection: List<Int>,
    callbacks: GameCallbacks,
    modifier: Modifier = Modifier,
    submitDimmed: Boolean = false,
    effects: GameEffects = remember { GameEffects() },
) {
    val k = kelimeColors
    val motion = LocalMotion.current
    val n = tiles.size
    val shake = remember { Animatable(0f) }
    LaunchedEffect(effects.shakeTick) {
        if (effects.shakeTick > 0 && motion.enabled) {
            // ±8 dp, 3 salınım, toplam 200 ms (doğrusal).
            shake.snapTo(0f)
            val step = 200 / 6
            for (target in listOf(1f, -1f, 1f, -1f, 1f, 0f)) shake.animateTo(target, tween(step, easing = LinearEasing))
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
        val w = maxWidth
        val diameter: Dp = if (w < 328.dp) w else minOf(w - 16.dp, 320.dp)
        val tokenSize: Dp = if (n >= 7) 56.dp else 64.dp
        val shuffleSize = 56.dp
        val fontScale = LocalDensity.current.fontScale
        val stackPrimary = w < 300.dp || fontScale > 1.15f
        val stackHints = w < 300.dp || fontScale > 1.5f
        val gap = 8.dp
        val ringColor = k.outline
        val tickColor = k.line
        val diskColor = k.surface

        Layout(
            content = {
                tiles.forEachIndexed { position, tile ->
                    key(tile.index) {
                        val slot = animateFloatAsState(
                            position.toFloat(), tween(motion.ms(220), easing = FastOutSlowInEasing), label = "slot",
                        )
                        LetterToken(
                            tile = tile,
                            badge = selection.indexOf(tile.index).takeIf { it >= 0 }?.plus(1),
                            size = tokenSize,
                            onClick = { callbacks.onLetter(tile.index) },
                            modifier = Modifier.then(SlotData(TokenSlot(slot))).semantics { traversalIndex = tile.index.toFloat() },
                        )
                    }
                }
                A11yButton(
                    "Gönder", "Gönder", callbacks.onSubmit,
                    Modifier.then(PartData(Part.Send)).semantics { traversalIndex = n + 0f },
                    minHeight = 64.dp, pill = true, dimmed = submitDimmed,
                )
                A11yButton(
                    "Sil", "Sil", callbacks.onDelete,
                    Modifier.then(PartData(Part.Delete)).semantics { traversalIndex = n + 1f },
                    minHeight = 56.dp, kind = ButtonKind.Secondary, contentPadding = PaddingValues(horizontal = 10.dp),
                )
                A11yButton(
                    "Temizle", "Temizle", callbacks.onClear,
                    Modifier.then(PartData(Part.Clear)).semantics { traversalIndex = n + 2f },
                    minHeight = 56.dp, kind = ButtonKind.Secondary, contentPadding = PaddingValues(horizontal = 10.dp),
                )
                ShuffleButton(
                    callbacks.onShuffle, shuffleSize,
                    Modifier.then(PartData(Part.Shuffle)).semantics { traversalIndex = n + 3f },
                )
                A11yButton(
                    GameEconomy.HINT_LETTER_DESCRIPTION, GameEconomy.HINT_LETTER_LABEL, callbacks.onHintLetter,
                    Modifier.then(PartData(Part.Hint)).semantics { traversalIndex = n + 4f },
                    minHeight = 56.dp, kind = ButtonKind.Secondary,
                )
                A11yButton(
                    GameEconomy.HINT_WORD_DESCRIPTION, GameEconomy.HINT_WORD_LABEL, callbacks.onHintWord,
                    Modifier.then(PartData(Part.Word)).semantics { traversalIndex = n + 5f },
                    minHeight = 56.dp, kind = ButtonKind.Secondary,
                )
            },
            modifier = Modifier.fillMaxWidth().drawBehind {
                val d = diameter.toPx()
                val c = Offset(size.width / 2 + shake.value * 8.dp.toPx(), d / 2)
                val radius = d / 2 - 1.dp.toPx()
                drawCircle(diskColor, radius, c)
                // 36 ince ölçek çizgisi (her onuncusu uzun).
                for (i in 0 until 36) {
                    val a = Math.toRadians(i * 10.0 - 90.0)
                    val len = if (i % 9 == 0) 12.dp.toPx() else 7.dp.toPx()
                    val r1 = radius - 3.dp.toPx()
                    val r0 = r1 - len
                    drawLine(
                        tickColor,
                        Offset(c.x + (r0 * cos(a)).toFloat(), c.y + (r0 * sin(a)).toFloat()),
                        Offset(c.x + (r1 * cos(a)).toFloat(), c.y + (r1 * sin(a)).toFloat()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                drawCircle(ringColor, radius, c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val d = diameter.roundToPx().coerceAtMost(width)
            val tPx = tokenSize.roundToPx()
            val sPx = shuffleSize.roundToPx()
            val gapPx = gap.roundToPx()
            val tokens = measurables.filter { it.parentData is TokenSlot }
            val tokenPlaceables = tokens.map { it.measure(Constraints.fixed(tPx, tPx)) }
            fun part(p: Part) = measurables.first { it.parentData == p }
            val shufflePlaceable = part(Part.Shuffle).measure(Constraints.fixed(sPx, sPx))

            // Eylem satırları: (parça, genişlik) listeleri; satır yüksekliği = en yüksek düğme.
            val sendW = (width * 0.45f).toInt()
            val rows: List<List<Pair<Part, Int>>> = buildList {
                if (stackPrimary) {
                    add(listOf(Part.Send to width))
                    add(listOf(Part.Delete to (width - gapPx) / 2, Part.Clear to (width - gapPx) / 2))
                } else {
                    val side = (width - sendW - 2 * gapPx) / 2
                    add(listOf(Part.Delete to side, Part.Send to sendW, Part.Clear to side))
                }
                if (stackHints) {
                    add(listOf(Part.Hint to width))
                    add(listOf(Part.Word to width))
                } else {
                    add(listOf(Part.Hint to (width - gapPx) / 2, Part.Word to (width - gapPx) / 2))
                }
            }
            val minHeights = mapOf(
                Part.Send to 64.dp, Part.Delete to 56.dp, Part.Clear to 56.dp, Part.Hint to 56.dp, Part.Word to 56.dp,
            )
            data class Placed(val part: Part, val x: Int, val y: Int, val p: Placeable)
            val placed = ArrayList<Placed>()
            var y = d + 16.dp.roundToPx()
            rows.forEachIndexed { rowIndex, row ->
                val natural = row.maxOf { (p, w) ->
                    maxOf(part(p).minIntrinsicHeight(w), minHeights.getValue(p).roundToPx())
                }
                var x = 0
                row.forEach { (p, w) ->
                    placed += Placed(p, x, y, part(p).measure(Constraints.fixed(w, natural)))
                    x += w + gapPx
                }
                // Sağa yaslı ilk satırda (Sil, Gönder, Temizle) x sayacı zaten genişlikleri toplar.
                y += natural + (if (rowIndex < rows.lastIndex) gapPx else 0)
            }
            val totalHeight = y

            layout(width, totalHeight) {
                val shakePx = (shake.value * 8.dp.toPx()).toInt()
                val cx = width / 2
                val cy = d / 2
                val r = d / 2f - tPx / 2f - 6.dp.toPx()
                val count = maxOf(n, 1)
                tokens.forEachIndexed { i, m ->
                    val slot = (m.parentData as TokenSlot).slot.value
                    val a = Math.toRadians(-90.0 + slot * 360.0 / count)
                    tokenPlaceables[i].place(
                        (cx + r * cos(a)).toInt() - tPx / 2 + shakePx,
                        (cy + r * sin(a)).toInt() - tPx / 2,
                    )
                }
                // Yerleştirme sırası (z-sırası) yazılış sırasıyla aynıdır: Gönder, Sil, Temizle, Karıştır, İpucu, Kelime aç.
                // Bu, anlamsal ağacın ağaç sırasını eski sırayla birebir tutar.
                fun put(p: Part) = placed.filter { it.part == p }.forEach { it.p.place(IntOffset(it.x, it.y)) }
                put(Part.Send); put(Part.Delete); put(Part.Clear)
                shufflePlaceable.place(cx - sPx / 2 + shakePx, cy - sPx / 2)
                put(Part.Hint); put(Part.Word)
            }
        }
    }
}

private class SlotData(val value: TokenSlot) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = value
}

private class PartData(val value: Part) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = value
}

/** Çarkın ortasındaki Karıştır düğmesi: pusula ibresi simgesi; etiket "Karıştır". */
@Composable
private fun ShuffleButton(onClick: () -> Unit, size: Dp, modifier: Modifier = Modifier) {
    val k = kelimeColors
    val interaction = remember { MutableInteractionSource() }
    androidx.compose.foundation.layout.Box(
        modifier.size(size).focusRing(interaction, size / 2)
            .clip(CircleShape).background(k.surface).border(2.dp, k.outline, CircleShape)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Karıştır" },
        contentAlignment = Alignment.Center,
    ) { KIcon(IconKind.Compass, k.primary, size = 30.dp) }
}
