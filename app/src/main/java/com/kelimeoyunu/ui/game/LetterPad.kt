package com.kelimeoyunu.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.ui.common.MinLetterHeight

const val LETTERS_PER_ROW = 4

/** Harf düğmeleri, 4'lü satırlarla sarılır. Seçili harf dolu, seçilmemiş çerçeveli gösterilir. */
@Composable
fun LetterPad(tiles: List<LetterTile>, onLetter: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(LETTERS_PER_ROW).forEach { rowTiles ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTiles.forEach { LetterButton(it, onLetter, Modifier.weight(1f)) }
                repeat(LETTERS_PER_ROW - rowTiles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LetterButton(tile: LetterTile, onLetter: (Int) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = if (tile.used) {
        ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary)
    } else {
        ButtonDefaults.buttonColors(containerColor = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer)
    }
    Button(
        onClick = { onLetter(tile.index) },
        modifier = modifier.heightIn(min = MinLetterHeight)
            .semantics { contentDescription = Announcements.letterButton(tile.char, tile.used) },
        colors = colors,
        border = BorderStroke(if (tile.used) 4.dp else 1.dp, scheme.outline),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
    ) {
        Text(TurkishText.upper(tile.char.toString()), Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.headlineMedium)
    }
}
