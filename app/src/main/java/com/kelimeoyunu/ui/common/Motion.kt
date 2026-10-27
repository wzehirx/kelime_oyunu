package com.kelimeoyunu.ui.common

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Hareket durumu: süreler tek yerden okunur. Sistem animasyon ölçeği 0 ya da ayarlardaki "Animasyonlar" kapalıysa
 * [enabled] false'tur ve her süre 0 ms'dir (anlık geçiş; konfeti ve sallanma yok).
 */
@Immutable
class Motion(val enabled: Boolean) {
    fun ms(millis: Int): Int = if (enabled) millis else 0
}

val LocalMotion = staticCompositionLocalOf { Motion(enabled = true) }

/** Sistem "Animatör süre ölçeği" 0 mı (geliştirici seçeneği ya da "animasyonları kaldır")? Okunamazsa false. */
fun animatorScaleIsZero(resolver: android.content.ContentResolver): Boolean = try {
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (_: Exception) {
    false
}

/** Sistem ölçeği değişince güncellenir. */
@Composable
fun rememberSystemMotionReduced(): Boolean {
    val context = LocalContext.current
    var reduced by remember { mutableStateOf(animatorScaleIsZero(context.contentResolver)) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = animatorScaleIsZero(resolver)
            }
        }
        try {
            resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        } catch (_: Exception) {
            // Gözlemci kurulamazsa ilk okunan değer kullanılır.
        }
        onDispose { try { resolver.unregisterContentObserver(observer) } catch (_: Exception) { } }
    }
    return reduced
}

@Composable
fun ProvideMotion(animationsSetting: Boolean, content: @Composable () -> Unit) {
    val reduced = rememberSystemMotionReduced()
    val motion = remember(animationsSetting, reduced) { Motion(enabled = animationsSetting && !reduced) }
    CompositionLocalProvider(LocalMotion provides motion, content = content)
}
