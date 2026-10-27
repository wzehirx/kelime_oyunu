package com.kelimeoyunu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.ButtonKind
import com.kelimeoyunu.ui.common.DangerButton
import com.kelimeoyunu.ui.common.MinLetterHeight
import com.kelimeoyunu.ui.common.SectionPanel
import com.kelimeoyunu.ui.common.SettingRow
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.kelimeColors

private fun onOff(enabled: Boolean) = if (enabled) "açık" else "kapalı"

fun soundSwitchDescription(enabled: Boolean) = "Ses efektleri: " + onOff(enabled)
fun vibrationSwitchDescription(enabled: Boolean) = "Titreşim: " + onOff(enabled)
fun animationsSwitchDescription(enabled: Boolean) = "Animasyonlar: " + onOff(enabled)

/**
 * Ayarlar. Anahtar satırları tek düğümdür: etiket ("Ses efektleri", "Titreşim", "Animasyonlar") ve durum
 * ("açık"/"kapalı"). İlerlemeyi sıfırlama ayrı, tehlike stilinde bir düğmedir ve onay diyaloğu ister.
 */
@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onResetConfirmed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vibrationEnabled: Boolean = true,
    onVibrationChange: (Boolean) -> Unit = {},
    animationsEnabled: Boolean = true,
    onAnimationsChange: (Boolean) -> Unit = {},
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Ayarlar",
            Modifier.initialFocus().semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            color = kelimeColors.ink,
        )
        SectionPanel {
            SettingRow("Ses efektleri", soundSwitchDescription(soundEnabled), soundEnabled, onSoundChange)
            SettingRow("Titreşim", vibrationSwitchDescription(vibrationEnabled), vibrationEnabled, onVibrationChange)
            SettingRow("Animasyonlar", animationsSwitchDescription(animationsEnabled), animationsEnabled, onAnimationsChange)
        }
        DangerButton("İlerlemeyi sıfırla", "İlerlemeyi sıfırla", { confirming = true }, Modifier.fillMaxWidth())
        A11yButton("Geri", "Geri", onBack, Modifier.fillMaxWidth(), minHeight = MinLetterHeight, kind = ButtonKind.Secondary)
    }
    if (confirming) {
        val k = kelimeColors
        AlertDialog(
            onDismissRequest = { confirming = false },
            containerColor = k.surface,
            title = { Text("İlerleme sıfırlansın mı?", style = KelimeType.section, color = k.ink) },
            text = { Text("Seviye, jeton ve bulunan kelimeler silinir. Bu işlem geri alınamaz.", style = KelimeType.body, color = k.ink) },
            confirmButton = {
                TextButton(
                    onClick = { confirming = false; onResetConfirmed() },
                    modifier = Modifier.heightIn(min = MinLetterHeight),
                    colors = ButtonDefaults.textButtonColors(contentColor = k.error),
                ) { Text("Evet, sıfırla", style = KelimeType.label) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirming = false },
                    modifier = Modifier.heightIn(min = MinLetterHeight),
                    colors = ButtonDefaults.textButtonColors(contentColor = k.primary),
                ) { Text("Vazgeç", style = KelimeType.label) }
            },
        )
    }
}
