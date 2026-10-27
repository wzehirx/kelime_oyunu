package com.kelimeoyunu.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.ui.common.IconKind
import com.kelimeoyunu.ui.common.KIcon
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.Radius
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * Kompakt kelime listesi: görsel olarak küçük (28 dp) harf kutuları, kısa kelimeler yan yana akar. TalkBack'te kelime
 * başına tek düğüm ve eski etiketler aynen ("Kelime 2, 5 harfli, açık harfler: _ A _ E _").
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordList(rows: List<WordRow>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier.fillMaxWidth().semantics { collectionInfo = CollectionInfo(rows.size, 1) },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { WordRowView(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordRowView(row: WordRow) {
    val k = kelimeColors
    FlowRow(
        Modifier.clearAndSetSemantics { contentDescription = Announcements.wordRowDescription(row) },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${row.number}.", Modifier.padding(end = 3.dp), style = KelimeType.small, color = k.inkMuted)
        row.cells.forEach { LetterBox(it, row.found) }
        // Bulundu bilgisi renkle değil, dolu kutu + kalın harf + onay işaretiyle de verilir.
        if (row.found) KIcon(IconKind.Check, k.success, size = 18.dp)
    }
}

@Composable
private fun LetterBox(letter: Char?, found: Boolean) {
    val k = kelimeColors
    Surface(
        color = if (letter != null) k.tile else k.surface,
        border = BorderStroke(if (found) 2.5.dp else 1.dp, k.outline),
        shape = RoundedCornerShape(Radius.cell),
        modifier = Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                letter?.let { TurkishText.upper(it.toString()) } ?: "",
                color = k.tileInk,
                fontWeight = FontWeight.Bold,
                style = KelimeType.label,
            )
        }
    }
}
