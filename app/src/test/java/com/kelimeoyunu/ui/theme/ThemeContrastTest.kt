package com.kelimeoyunu.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private fun lin(v: Float): Double = v.toDouble().let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
    private fun lum(c: Color) = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = lum(a).let { x -> lum(b).let { y -> maxOf(x, y) to minOf(x, y) } }
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(name: String, c: ColorScheme) {
        val pairs = mapOf(
            "onPrimary/primary" to (c.onPrimary to c.primary),
            "onPrimaryContainer/primaryContainer" to (c.onPrimaryContainer to c.primaryContainer),
            "onSecondary/secondary" to (c.onSecondary to c.secondary),
            "onSecondaryContainer/secondaryContainer" to (c.onSecondaryContainer to c.secondaryContainer),
            "onBackground/background" to (c.onBackground to c.background),
            "onSurface/surface" to (c.onSurface to c.surface),
            "onSurfaceVariant/surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
            "onError/error" to (c.onError to c.error),
            "error/background" to (c.error to c.background),
            "primary/background" to (c.primary to c.background),
            "outline/background" to (c.outline to c.background),
        )
        pairs.forEach { (n, p) ->
            val r = ratio(p.first, p.second)
            assertTrue("$name $n kontrast $r < 4.5", r >= 4.5)
        }
    }

    @Test fun lightMeetsAa() = check("açık", LightColors)
    @Test fun darkMeetsAa() = check("koyu", DarkColors)
}
