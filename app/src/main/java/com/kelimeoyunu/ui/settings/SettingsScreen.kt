package com.kelimeoyunu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.MinLetterHeight

fun soundSwitchDescription(enabled: Boolean) = "Ses efektleri: " + if (enabled) "açık" else "kapalı"

@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onResetConfirmed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = MinLetterHeight)
                .toggleable(value = soundEnabled, role = Role.Switch, onValueChange = onSoundChange)
                .semantics {
                    contentDescription = "Ses efektleri"
                    stateDescription = if (soundEnabled) "açık" else "kapalı"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(soundSwitchDescription(soundEnabled), Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleMedium)
            Switch(checked = soundEnabled, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
        }
        A11yButton(
            "İlerlemeyi sıfırla", "İlerlemeyi sıfırla", { confirming = true },
            Modifier.fillMaxWidth(), minHeight = MinLetterHeight,
        )
        A11yButton("Geri", "Geri", onBack, Modifier.fillMaxWidth(), minHeight = MinLetterHeight)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("İlerleme sıfırlansın mı?") },
            text = { Text("Seviye, jeton ve bulunan kelimeler silinir. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = { confirming = false; onResetConfirmed() },
                    modifier = Modifier.heightIn(min = MinLetterHeight),
                ) { Text("Evet, sıfırla") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirming = false },
                    modifier = Modifier.heightIn(min = MinLetterHeight),
                ) { Text("Vazgeç") }
            },
        )
    }
}
