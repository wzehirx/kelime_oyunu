package com.kelimeoyunu.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.domain.TurkishText

/**
 * "Oluşturulan kelime: KAL" alanı. Bilerek liveRegion DEĞİL: değişiklikleri Announcer okur,
 * canlı bölge de okusaydı her harf iki kez duyulurdu.
 */
@Composable
fun CurrentWordField(word: String, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
            .clearAndSetSemantics { contentDescription = Announcements.currentWordDescription(word) },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Text(
                if (word.isEmpty()) "Harf seç" else TurkishText.upper(word),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
