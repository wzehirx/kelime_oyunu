package com.kelimeoyunu.a11y

import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Ekran açılınca bu öğeye giriş odağı verir; TalkBack erişilebilirlik odağını giriş odağına
 * taşıdığı için ekranın başlığı/ilk öğesi okunur. Düğmelerde de `focusable()`
 * gerekir: dokunma kipinde tıklanabilir öğeler varsayılan olarak odak alamaz. Odak istenemezse sessizce vazgeçilir.
 */
fun Modifier.initialFocus(): Modifier = composed {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) {
        try {
            requester.requestFocus()
        } catch (_: IllegalStateException) {
            // Öğe henüz yerleşmediyse odak istemeyiz.
        }
    }
    focusRequester(requester).focusable()
}
