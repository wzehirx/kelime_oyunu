package com.kelimeoyunu.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val MinActionHeight: Dp = 48.dp
val MinLetterHeight: Dp = 56.dp

/**
 * Erişilebilir düğme: TalkBack [description]'ı okur; görünen [label] ekran okuyucudan gizlenir
 * (çift okuma olmasın). Yükseklik en az [minHeight], büyük yazı tipinde büyür.
 */
@Composable
fun A11yButton(
    description: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = MinActionHeight,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = minHeight).semantics { contentDescription = description },
        colors = colors,
    ) {
        Text(label, Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

val ButtonGap = Arrangement.spacedBy(8.dp)
