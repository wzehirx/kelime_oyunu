package com.kelimeoyunu.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Görsel belirteçler (docs/superpowers/specs/2026-10-27-visual-handoff.md). Açık tema "Deniz kâğıdı",
 * koyu tema "Gece haritası". Her metin/zemin çifti ThemeContrastTest ile iki temada ayrı ayrı doğrulanır.
 *
 * [line] yalnızca süs ayraçları ve ızgara çizgileri içindir (bilgi taşımaz, 3:1 aranmaz). Bileşen sınırı
 * gereken yerlerde (jeton kenarı, anahtar, tehlike düğmesi, çark halkası) [outline] kullanılır (>= 4.5:1).
 * Bölge mürekkepleri yalnızca grafikte (halka, çerçeve, dolgu) kullanılır; üzerlerindeki metin [ink] rengindedir.
 */
@Immutable
class KelimeColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val ink: Color,
    val inkMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    /** Birincil mavinin yüzey üzerindeki hafif tonu (ipucuyla açılan hücre). */
    val primaryTint: Color,
    val line: Color,
    val outline: Color,
    val tile: Color,
    val tileInk: Color,
    /** Jeton kenarı: açık temada [outline], koyu temada jeton zaten zeminden ayrıldığı için jetonun kendisi. */
    val tileEdge: Color,
    val success: Color,
    val error: Color,
    val onError: Color,
    val avrupa: Color,
    val asya: Color,
    val ortadogu: Color,
    val afrika: Color,
    val amerika: Color,
    val okyanusya: Color,
) {
    /** places.json `bolge` değerine göre damga mürekkebi; bilinmeyen bölge birincil renge düşer. */
    fun regionInk(bolge: String?): Color = when (bolge) {
        "avrupa" -> avrupa
        "asya" -> asya
        "ortadogu" -> ortadogu
        "afrika" -> afrika
        "amerika" -> amerika
        "okyanusya" -> okyanusya
        else -> primary
    }

    val regionInks: List<Color> get() = listOf(avrupa, asya, ortadogu, afrika, amerika, okyanusya)
}

val LightTokens = KelimeColors(
    bg = Color(0xFFE8F0EE), surface = Color(0xFFFAFCFB), surfaceVariant = Color(0xFFDCE7E4),
    ink = Color(0xFF14232B), inkMuted = Color(0xFF3D5159),
    primary = Color(0xFF1F4E79), onPrimary = Color(0xFFFFFFFF), primaryTint = Color(0xFFD9E2E8),
    line = Color(0xFFB9CBC8), outline = Color(0xFF4A5F67),
    tile = Color(0xFFF3EEDF), tileInk = Color(0xFF14232B), tileEdge = Color(0xFF4A5F67),
    success = Color(0xFF1F7A52), error = Color(0xFFB3323A), onError = Color(0xFFFFFFFF),
    avrupa = Color(0xFF3F63D9), asya = Color(0xFFD6406C), ortadogu = Color(0xFFC97316),
    afrika = Color(0xFF1F8F5F), amerika = Color(0xFF8A4FD3), okyanusya = Color(0xFF0F97AA),
)

/** Koyu temada bölge mürekkepleri açık temadakinin %25 açılmış hâlidir. */
val DarkTokens = KelimeColors(
    bg = Color(0xFF0F2229), surface = Color(0xFF17313A), surfaceVariant = Color(0xFF20404A),
    ink = Color(0xFFE6F0EE), inkMuted = Color(0xFFA9C2C6),
    primary = Color(0xFF8DB8E8), onPrimary = Color(0xFF0F2229), primaryTint = Color(0xFF3A5A6E),
    line = Color(0xFF2C4A55), outline = Color(0xFF8EA9B0),
    tile = Color(0xFFF3EEDF), tileInk = Color(0xFF14232B), tileEdge = Color(0xFFF3EEDF),
    success = Color(0xFF5BCB93), error = Color(0xFFFF8F94), onError = Color(0xFF0F2229),
    avrupa = Color(0xFF6F8AE2), asya = Color(0xFFE07091), ortadogu = Color(0xFFD69650),
    afrika = Color(0xFF57AB87), amerika = Color(0xFFA77BDE), okyanusya = Color(0xFF4BB1BF),
)

val LocalKelimeColors = staticCompositionLocalOf { LightTokens }

/** space-xs/sm/md/lg/xl */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** Köşe yarıçapları: panel 14 dp, hücre 6 dp, hap ve jeton tam yuvarlak; satırlarda köşe yok. */
object Radius {
    val panel = 14.dp
    val cell = 6.dp
    val pill = 999.dp
}

object Elevation {
    val token = 3.dp
    val panel = 2.dp
}

/** Dokunma hedefi alt sınırı ve jeton/birincil düğme ölçüleri. */
object Touch {
    val min = 48.dp
    val token = 64.dp
    val tokenSmall = 56.dp
    val primary = 64.dp
}
