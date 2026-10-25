package com.kelimeoyunu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Yüksek kontrastlı renkler; her metin/arka plan çifti en az 4.5:1 (ThemeContrastTest doğrular). */
val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0B3D91), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF), onPrimaryContainer = Color(0xFF001B44),
    secondary = Color(0xFF1F3A5F), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3EAF7), onSecondaryContainer = Color(0xFF0A1A33),
    background = Color(0xFFFFFFFF), onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEDEDED), onSurfaceVariant = Color(0xFF222222),
    outline = Color(0xFF444444),
    error = Color(0xFF9B0000), onError = Color(0xFFFFFFFF),
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF9DC3FF), onPrimary = Color(0xFF00204D),
    primaryContainer = Color(0xFF1B4A99), onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFB7CCEA), onSecondary = Color(0xFF0A1A33),
    secondaryContainer = Color(0xFF1E2A3F), onSecondaryContainer = Color(0xFFEAF1FF),
    background = Color(0xFF000000), onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF000000), onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF262626), onSurfaceVariant = Color(0xFFF0F0F0),
    outline = Color(0xFFBBBBBB),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)

@Composable
fun KelimeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
