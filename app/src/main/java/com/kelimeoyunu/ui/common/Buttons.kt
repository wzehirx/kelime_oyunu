package com.kelimeoyunu.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.ui.theme.Radius
import com.kelimeoyunu.ui.theme.kelimeColors

val MinActionHeight: Dp = 48.dp
val MinLetterHeight: Dp = 56.dp

/** Düğme görünümü: birincil (mavi dolgu), ikincil (yüzey + çerçeve) ve tehlike (hata rengi çerçeve). */
enum class ButtonKind { Primary, Secondary, Danger }

/**
 * Erişilebilir düğme: TalkBack [description]'ı okur; görünen [label] ekran okuyucudan gizlenir
 * (çift okuma olmasın). Yükseklik en az [minHeight], büyük yazı tipinde büyür. Odaktayken görünür halka çizilir.
 * [pill] true ise tam yuvarlak köşe (birincil eylemler), değilse 12 dp.
 */
@Composable
fun A11yButton(
    description: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = MinActionHeight,
    kind: ButtonKind = ButtonKind.Primary,
    pill: Boolean = false,
    dimmed: Boolean = false,
    colors: ButtonColors? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
) {
    val k = kelimeColors
    val radius = if (pill) Radius.pill else 12.dp
    val interaction = remember { MutableInteractionSource() }
    val resolved = colors ?: when (kind) {
        ButtonKind.Primary -> ButtonDefaults.buttonColors(
            containerColor = k.primary, contentColor = k.onPrimary,
        )
        ButtonKind.Secondary -> ButtonDefaults.buttonColors(containerColor = k.surface, contentColor = k.ink)
        ButtonKind.Danger -> ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = k.error)
    }
    val border = when (kind) {
        ButtonKind.Primary -> if (dimmed) BorderStroke(2.dp, k.primary) else null
        ButtonKind.Secondary -> BorderStroke(1.5.dp, k.outline)
        ButtonKind.Danger -> BorderStroke(2.dp, k.error)
    }
    // "Soluk" yalnızca görseldir (boş Gönder gibi): düğme etkin kalır, anlamsal olarak devre dışı değildir.
    val finalColors = if (dimmed && kind == ButtonKind.Primary) {
        ButtonDefaults.buttonColors(containerColor = k.surface, contentColor = k.primary)
    } else {
        resolved
    }
    Button(
        onClick = onClick,
        modifier = modifier.focusRing(interaction, radius).heightIn(min = minHeight)
            .semantics { contentDescription = description },
        colors = finalColors,
        shape = RoundedCornerShape(radius),
        border = border,
        interactionSource = interaction,
        contentPadding = contentPadding,
    ) {
        Text(label, Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

/** Tehlike düğmesi (ör. İlerlemeyi sıfırla): ayrı, hata rengi çerçeveli; onay diyaloğu çağıranın işidir. */
@Composable
fun DangerButton(
    description: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = MinLetterHeight,
) = A11yButton(description, label, onClick, modifier, minHeight, kind = ButtonKind.Danger)

val ButtonGap = Arrangement.spacedBy(8.dp)
