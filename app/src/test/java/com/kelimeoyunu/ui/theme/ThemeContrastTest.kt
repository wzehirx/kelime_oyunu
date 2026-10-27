package com.kelimeoyunu.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.1 AA, iki temada ayrı ayrı: metin >= 4.5:1, bileşen/grafik >= 3:1. Material şeması (eski sözleşme) ve v2.0
 * belirteçleri (bölge damga mürekkepleri dahil) taranır.
 *
 * Bilinçli muafiyet: `line` (color-line) yalnızca süs ayraçlarında ve kapalı ızgara hücresinin çizgisinde kullanılır;
 * bilgi taşımaz (ızgara ekran okuyucudan gizlidir, bilgi kelime listesindedir, listenin kutuları `outline` ile çizilir),
 * bu yüzden 3:1 aranmaz; `line` ise yalnızca ilerleme izi ve ayraçta durur. Bölge mürekkepleri yalnızca grafikte
 * (halka, çerçeve, dolgu) kullanılır, bu nedenle 3:1 ile sınanır; üzerlerindeki metin `ink` rengindedir (4.5:1 ayrıca sınanır).
 */
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

    private val themes = mapOf("açık" to LightTokens, "koyu" to DarkTokens)

    private fun assertText(theme: String, name: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue("$theme metin $name kontrast ${"%.2f".format(r)} < 4.5", r >= 4.5)
    }

    private fun assertGraphic(theme: String, name: String, fg: Color, bg: Color) {
        val r = ratio(fg, bg)
        assertTrue("$theme bileşen $name kontrast ${"%.2f".format(r)} < 3", r >= 3.0)
    }

    private fun regions(t: KelimeColors) = mapOf(
        "avrupa" to t.avrupa, "asya" to t.asya, "ortadogu" to t.ortadogu,
        "afrika" to t.afrika, "amerika" to t.amerika, "okyanusya" to t.okyanusya,
    )

    @Test fun textPairsMeet45InBothThemes() {
        themes.forEach { (n, t) ->
            assertText(n, "ink/bg", t.ink, t.bg)
            assertText(n, "ink/surface", t.ink, t.surface)
            assertText(n, "ink/surfaceVariant", t.ink, t.surfaceVariant)
            assertText(n, "inkMuted/bg", t.inkMuted, t.bg)
            assertText(n, "inkMuted/surface", t.inkMuted, t.surface)
            assertText(n, "inkMuted/surfaceVariant", t.inkMuted, t.surfaceVariant)
            assertText(n, "onPrimary/primary (birincil düğme, seçili jeton, sıra)", t.onPrimary, t.primary)
            assertText(n, "primary/bg", t.primary, t.bg)
            assertText(n, "primary/surface (soluk Gönder, Menü)", t.primary, t.surface)
            assertText(n, "tileInk/tile (jeton harfi, bulunan hücre)", t.tileInk, t.tile)
            assertText(n, "ink/primaryTint (ipucuyla açılan hücre)", t.ink, t.primaryTint)
            assertText(n, "bg/ink (sıra rozeti)", t.bg, t.ink)
            assertText(n, "success/bg", t.success, t.bg)
            assertText(n, "success/surface", t.success, t.surface)
            assertText(n, "error/bg", t.error, t.bg)
            assertText(n, "error/surface (tehlike düğmesi)", t.error, t.surface)
            assertText(n, "onError/error", t.onError, t.error)
            assertText(n, "outline/bg", t.outline, t.bg)
            assertText(n, "outline/surface", t.outline, t.surface)
        }
    }

    @Test fun stampAndPillTextOnRegionTintMeets45() {
        themes.forEach { (n, t) ->
            regions(t).forEach { (name, ink) ->
                // Damga zemini: bölge mürekkebinin %10'u yüzey üzerinde; harfler `ink` rengindedir.
                val tint = ink.copy(alpha = 0.10f).compositeOver(t.surface)
                assertText(n, "ink/damga zemini($name)", t.ink, tint)
                val tintBg = ink.copy(alpha = 0.10f).compositeOver(t.bg)
                assertText(n, "ink/bg üstünde damga($name)", t.ink, tintBg)
            }
        }
    }

    @Test fun regionInksAreGraphicsAtLeast3to1OnBackgroundAndSurface() {
        themes.forEach { (n, t) ->
            regions(t).forEach { (name, ink) ->
                assertGraphic(n, "$name/bg", ink, t.bg)
                assertGraphic(n, "$name/surface", ink, t.surface)
            }
        }
    }

    @Test fun componentBoundariesMeet3to1() {
        themes.forEach { (n, t) ->
            assertGraphic(n, "outline/bg (ikincil düğme, anahtar kenarı)", t.outline, t.bg)
            assertGraphic(n, "outline/surface", t.outline, t.surface)
            assertGraphic(n, "outline/surfaceVariant", t.outline, t.surfaceVariant.compositeOver(t.surface))
            assertGraphic(n, "primary/bg", t.primary, t.bg)
            assertGraphic(n, "primary/surface (odak halkası, soluk Gönder çerçevesi)", t.primary, t.surface)
            assertGraphic(n, "primary/bg (odak halkası)", t.primary, t.bg)
            assertGraphic(n, "onPrimary/primary (anahtar başparmağı)", t.onPrimary, t.primary)
            assertGraphic(n, "success/surface (simge)", t.success, t.surface)
            assertGraphic(n, "error/surface (tehlike çerçevesi)", t.error, t.surface)
            assertGraphic(n, "inkMuted/surface (kilit simgesi)", t.inkMuted, t.surface)
            // Jeton kenarı: açık temada `tileEdge` jetonu zeminden ayırır; koyu temada jetonun kendisi zeminle yeterince ayrışır.
            assertGraphic(n, "tileEdge/bg", t.tileEdge, t.bg)
            assertGraphic(n, "tile/bg (jeton ya da kenarı)", if (n == "açık") t.tileEdge else t.tile, t.bg)
            assertGraphic(n, "tileEdge/primaryTint (ipucu hücresi kenarı)", t.tileEdge, t.primaryTint)
            assertGraphic(n, "tileEdge/surface (çark üzerinde jeton)", t.tileEdge, t.surface)
        }
    }

    @Test fun darkRegionInksAreTheLightOnesLightenedBy25Percent() {
        fun lighten(c: Color) = Color(c.red + (1 - c.red) * 0.25f, c.green + (1 - c.green) * 0.25f, c.blue + (1 - c.blue) * 0.25f)
        val light = regions(LightTokens)
        val dark = regions(DarkTokens)
        light.forEach { (name, c) ->
            val want = lighten(c)
            val got = dark.getValue(name)
            assertEquals("$name R", want.red, got.red, 0.01f)
            assertEquals("$name G", want.green, got.green, 0.01f)
            assertEquals("$name B", want.blue, got.blue, 0.01f)
        }
    }

    @Test fun regionInkLookupCoversAllSixRegionsAndFallsBack() {
        val t = LightTokens
        assertEquals(t.avrupa, t.regionInk("avrupa"))
        assertEquals(t.ortadogu, t.regionInk("ortadogu"))
        assertEquals(t.okyanusya, t.regionInk("okyanusya"))
        assertEquals(t.primary, t.regionInk("bilinmeyen"))
        assertEquals(6, t.regionInks.toSet().size)
    }
}
