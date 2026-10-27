package com.kelimeoyunu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun scheme(t: KelimeColors, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = t.primary, onPrimary = t.onPrimary,
        primaryContainer = t.primaryTint, onPrimaryContainer = t.ink,
        secondary = t.primary, onSecondary = t.onPrimary,
        secondaryContainer = t.tile, onSecondaryContainer = t.tileInk,
        tertiary = t.success, onTertiary = t.onPrimary,
        background = t.bg, onBackground = t.ink,
        surface = t.surface, onSurface = t.ink,
        surfaceVariant = t.surfaceVariant, onSurfaceVariant = t.ink,
        surfaceContainer = t.surface, surfaceContainerHigh = t.surface, surfaceContainerHighest = t.surfaceVariant,
        outline = t.outline, outlineVariant = t.line,
        error = t.error, onError = t.onError,
    )
}

/** Belirteçlerden türetilen Material renk şemaları (Material yalnızca temel bileşenlerde kullanılır). */
val LightColors: ColorScheme = scheme(LightTokens, dark = false)
val DarkColors: ColorScheme = scheme(DarkTokens, dark = true)

/** Güncel tema belirteçleri: `KelimeTheme` içinde `kelimeColors` ile okunur. */
val kelimeColors: KelimeColors
    @Composable get() = LocalKelimeColors.current

@Composable
fun KelimeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val tokens = if (darkTheme) DarkTokens else LightTokens
    CompositionLocalProvider(LocalKelimeColors provides tokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = KelimeTypography,
            content = content,
        )
    }
}
