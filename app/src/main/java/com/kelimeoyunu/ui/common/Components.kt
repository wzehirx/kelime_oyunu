package com.kelimeoyunu.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.ui.theme.Elevation
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.Radius
import com.kelimeoyunu.ui.theme.Space
import com.kelimeoyunu.ui.theme.kelimeColors

/** Ülke adından damga yazısı: tek sözcükte ilk iki harf, çok sözcükte ilk iki sözcüğün baş harfi; büyük harf. */
fun placeInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val raw = when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(2)
        else -> words[0].take(1) + words[1].take(1)
    }
    return TurkishText.upper(raw)
}

/**
 * Ülke damgası: 2,5 dp bölge mürekkebi halka + ince iç halka, ortada iki harf, −4° dönüklük. Yalnızca süstür
 * (ekran okuyucudan gizli). Kilitliyse gri halka ve kilit simgesi. Harfler mürekkep renginde (kontrast için).
 */
@Composable
fun Stamp(
    initials: String,
    ink: Color,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    size: Dp = 56.dp,
    rotation: Float = -4f,
) {
    val k = kelimeColors
    val ring = if (locked) k.outline else ink
    Box(
        modifier.size(size).rotate(rotation).clearAndSetSemantics {}
            .drawBehind {
                drawCircle(ring.copy(alpha = if (locked) 0.12f else 0.10f))
                drawCircle(ring, radius = this.size.minDimension / 2 - 1.25.dp.toPx(), style = Stroke(2.5.dp.toPx()))
                drawCircle(ring, radius = this.size.minDimension / 2 - 6.dp.toPx(), style = Stroke(1.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        if (locked) {
            KIcon(IconKind.Lock, k.inkMuted, size = size * 0.4f)
        } else {
            CappedFontScale(1.2f) {
                Text(initials, style = KelimeType.stamp, color = k.ink, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

/** 4 dp yüksekliğinde ilerleme çizgisi; dekoratiftir (bilgi başlıkta metinle verilir). */
@Composable
fun ProgressLine(fraction: Float, fill: Color, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val track = kelimeColors.line
    Canvas(modifier.fillMaxWidth().height(height).clearAndSetSemantics {}) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(track, size = size, cornerRadius = r)
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 0f) drawRoundRect(fill, size = Size(maxOf(w, size.height), size.height), cornerRadius = r)
    }
}

/** Halka ilerleme (ör. "12 / 100 ülke"); dekoratif. */
@Composable
fun ProgressRing(fraction: Float, ink: Color, modifier: Modifier = Modifier, size: Dp = 40.dp, stroke: Dp = 5.dp) {
    val track = kelimeColors.line
    Canvas(modifier.size(size).clearAndSetSemantics {}) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = Size(this.size.width - w, this.size.height - w)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        val sweep = 360f * fraction.coerceIn(0f, 1f)
        if (sweep > 0f) drawArc(ink, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/**
 * Durum/bilgi hapı: simge + metin (renk tek başına bilgi taşımaz). [border] verilirse çerçeve rengi (ülke hapında
 * bölge mürekkebi); [progress] verilirse altta 2 dp ilerleme çizgisi. Dekoratif kullanımda çağıran gizler.
 */
@Composable
fun StatPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: IconKind? = null,
    iconTint: Color = kelimeColors.ink,
    border: Color = kelimeColors.outline,
    progress: Float? = null,
    progressColor: Color = border,
    maxLines: Int = 1,
) {
    val k = kelimeColors
    Surface(
        modifier,
        shape = RoundedCornerShape(Radius.pill),
        color = k.surface,
        contentColor = k.ink,
        border = BorderStroke(1.5.dp, border),
    ) {
        Column(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max)) {
            Row(
                Modifier.heightIn(min = 32.dp).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) KIcon(icon, iconTint, size = 16.dp)
                Text(text, style = KelimeType.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
            }
            if (progress != null) {
                ProgressLine(progress, progressColor, Modifier.padding(horizontal = 10.dp).padding(bottom = 4.dp), height = 2.dp)
            }
        }
    }
}

/**
 * "Devam et" kahraman paneli: ülke damgası, başlık, alt başlık, ilerleme çubuğu ve altta büyük birincil düğme görünümü.
 * TEK anlamsal düğümdür: [description] okunur, içindekiler gizlidir. İç "düğme" yalnızca görseldir.
 */
@Composable
fun HeroPanel(
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    progress: Float? = null,
    progressColor: Color = kelimeColors.primary,
    leading: @Composable () -> Unit = {},
) {
    val k = kelimeColors
    val shape = RoundedCornerShape(Radius.panel)
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier.focusRing(interaction, Radius.panel).clip(shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        shape = shape,
        color = k.surface,
        shadowElevation = Elevation.panel,
        border = BorderStroke(1.dp, k.line),
    ) {
        Column(Modifier.clearAndSetSemantics {}.padding(Space.md), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                leading()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(title, style = KelimeType.title, color = k.ink)
                    if (subtitle != null) Text(subtitle, style = KelimeType.body, color = k.inkMuted)
                }
            }
            if (progress != null) ProgressLine(progress, progressColor)
            Box(
                Modifier.fillMaxWidth().heightIn(min = Touch64).clip(RoundedCornerShape(Radius.pill)).background(k.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    actionLabel, Modifier.padding(horizontal = Space.md, vertical = Space.sm),
                    style = KelimeType.section, color = k.onPrimary, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val Touch64 = 64.dp

/**
 * Menü/ülke satırı: kart değil, alt çizgili defter satırı (köşe yok). Tek anlamsal düğümdür; içerik gizlidir.
 * [highlight] true ise birincil çerçeve (mevcut ülke).
 */
@Composable
fun ListRow(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    highlight: Boolean = false,
    content: @Composable () -> Unit,
) {
    val k = kelimeColors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier.focusRing(interaction, 4.dp).fillMaxWidth().heightIn(min = Touch64)
            .then(if (highlight) Modifier.border(2.dp, k.primary) else Modifier)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .drawBehind {
                drawLine(k.line, Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), 1.dp.toPx())
            }
            .padding(vertical = Space.sm, horizontal = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Box(Modifier.clearAndSetSemantics {}) { leading() }
        Column(Modifier.weight(1f).clearAndSetSemantics {}, verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
        Box(Modifier.clearAndSetSemantics {}) { trailing() }
    }
}

/**
 * Anahtar satırı. Anlamsal: etiket [name], durum "açık/kapalı" (stateDescription); görünen metin gizlidir.
 */
@Composable
fun SettingRow(
    name: String,
    visibleText: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val k = kelimeColors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier.focusRing(interaction, 8.dp).fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value = checked, interactionSource = interaction, indication = LocalIndication.current, role = Role.Switch, onValueChange = onChange)
            .semantics {
                contentDescription = name
                stateDescription = if (checked) "açık" else "kapalı"
            }
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(visibleText, Modifier.weight(1f).clearAndSetSemantics {}, style = KelimeType.body, color = k.ink)
        Spacer(Modifier.width(Space.md))
        Switch(
            checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {},
            colors = SwitchDefaults.colors(
                checkedThumbColor = k.onPrimary, checkedTrackColor = k.primary, checkedBorderColor = k.primary,
                uncheckedThumbColor = k.outline, uncheckedTrackColor = k.surface, uncheckedBorderColor = k.outline,
            ),
        )
    }
}

/** Bölüm paneli (ayarlar): yüzey rengi, 14 dp köşe, 2 dp yükselti. */
@Composable
fun SectionPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val k = kelimeColors
    Surface(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.panel), color = k.surface,
        shadowElevation = Elevation.panel, border = BorderStroke(1.dp, k.line),
    ) { Column { content() } }
}

/** Yuvarlak simge rozeti (menü satırlarında soldaki simge). */
@Composable
fun IconBadge(kind: IconKind, modifier: Modifier = Modifier, tint: Color = kelimeColors.primary) {
    Box(
        modifier.size(48.dp).clip(CircleShape).background(kelimeColors.surfaceVariant).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) { KIcon(kind, tint, size = 24.dp) }
}
