package com.kelimeoyunu.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.domain.GameEconomy
import com.kelimeoyunu.ui.common.A11yButton

private class Action(val description: String, val label: String, val onClick: () -> Unit)

/**
 * Sabit sıra ve konum: Gönder, Sil, Temizle, Karıştır, İpucu, Kelime aç (iki sütun, soldan sağa).
 * Yetersiz jetonda düğmeler pasif yapılmaz; basınca neden söylenir.
 */
@Composable
fun ActionBar(c: GameCallbacks, modifier: Modifier = Modifier) {
    val actions = listOf(
        Action("Gönder", "Gönder", c.onSubmit),
        Action("Sil", "Sil", c.onDelete),
        Action("Temizle", "Temizle", c.onClear),
        Action("Karıştır", "Karıştır", c.onShuffle),
        Action(GameEconomy.HINT_LETTER_DESCRIPTION, GameEconomy.HINT_LETTER_LABEL, c.onHintLetter),
        Action(GameEconomy.HINT_WORD_DESCRIPTION, GameEconomy.HINT_WORD_LABEL, c.onHintWord),
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { A11yButton(it.description, it.label, it.onClick, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}
