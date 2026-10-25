package com.kelimeoyunu.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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

/** Kelime listesi: görsel olarak kutu kutu harfler, TalkBack'te kelime başına tek düğüm. */
@Composable
fun WordList(rows: List<WordRow>, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().semantics { collectionInfo = CollectionInfo(rows.size, 1) },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { WordRowView(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordRowView(row: WordRow) {
    FlowRow(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = Announcements.wordRowDescription(row) },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${row.number}.", Modifier.padding(end = 4.dp), style = MaterialTheme.typography.bodyLarge)
        row.cells.forEach { LetterBox(it, row.found) }
        // Bulundu bilgisi renkle değil, dolu kutu + kalın harf + onay işaretiyle de verilir.
        if (row.found) Text("✓", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LetterBox(letter: Char?, found: Boolean) {
    val color = if (letter != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
    Surface(
        color = color,
        border = BorderStroke(if (found) 3.dp else 1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                letter?.let { TurkishText.upper(it.toString()) } ?: "",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
