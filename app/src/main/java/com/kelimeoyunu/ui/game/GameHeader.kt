package com.kelimeoyunu.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.ui.common.IconKind
import com.kelimeoyunu.ui.common.KIcon
import com.kelimeoyunu.ui.common.StatPill
import com.kelimeoyunu.ui.common.focusRing
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.Space
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * Üst çubuk başlığı. ("Menü" düğmesi [MenuChip], odak sırasının sonunda ayrı bir düğümdür.) Seviye, ülke hapı, jeton hapı, kalan kelime ve bonus sayacı TEK başlık
 * düğümüdür ve eski açıklamayı okur; içindeki görsel metinler gizlidir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameHeader(state: GameUiState.Ready, modifier: Modifier = Modifier) {
    val k = kelimeColors
    val description = Announcements.headerDescription(
        state.levelNumber, state.coins, state.bonusMeter, state.bonusTarget, state.remainingWords, state.place,
    )
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().initialFocus().semantics(mergeDescendants = true) {
                heading()
                contentDescription = description
            },
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Seviye ${state.levelNumber}", Modifier.clearAndSetSemantics {}, style = KelimeType.title, color = k.ink)
                state.place?.let {
                    StatPill(
                        Announcements.placeProgress(it), Modifier.clearAndSetSemantics {},
                        border = k.regionInk(it.place.bolge),
                        progress = it.levelInPlace.toFloat() / it.placeLevelCount,
                        progressColor = k.regionInk(it.place.bolge),
                    )
                }
                StatPill("${state.coins}", Modifier.clearAndSetSemantics {}, icon = IconKind.Coin, iconTint = k.primary)
            }
            Text(
                "Kalan kelime: ${state.remainingWords}   Bonus: ${state.bonusMeter} / ${state.bonusTarget}",
                Modifier.clearAndSetSemantics {},
                style = KelimeType.small, color = k.inkMuted,
            )
        }
    }
}

/**
 * Görünür "Menü" düğmesi; TalkBack'te "Ana menüye dön" etiketiyle, odak sırasının SONUNDA okunur (çağıran bunu
 * son çocuk olarak yazar ve büyük bir traversalIndex verir; mevcut sıra bozulmaz). Metin ve simge gizlidir.
 */
@Composable
fun MenuChip(onMenu: () -> Unit, modifier: Modifier = Modifier) {
    val k = kelimeColors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier.focusRing(interaction, 24.dp).defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, role = Role.Button, onClick = onMenu)
            .semantics { contentDescription = "Ana menüye dön" }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KIcon(IconKind.Menu, k.primary, size = 22.dp)
        Text("Menü", Modifier.clearAndSetSemantics {}, style = KelimeType.label, color = k.primary)
    }
}
