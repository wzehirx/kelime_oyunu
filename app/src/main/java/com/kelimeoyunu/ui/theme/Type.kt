package com.kelimeoyunu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kelimeoyunu.R

/**
 * Tek aile: Atkinson Hyperlegible Next (OFL), Regular ve Bold; res/font altında gömülüdür. Türkçe harfler
 * `tools/fonts/verify_font_glyphs.py` ile doğrulanır. Boyutlar sp'dir (sistem yazı boyutuna uyar).
 */
val AtkinsonFamily = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
)

private fun style(size: Int, bold: Boolean, lineHeight: Float = 1.3f) = TextStyle(
    fontFamily = AtkinsonFamily,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
)

/** display 32 / title 28 / section 20 / body 18 (satır 1.4) / label 16 / small 14 / tile 30 / stamp 16 */
object KelimeType {
    val display = style(32, bold = true)
    val title = style(28, bold = true)
    val section = style(20, bold = true)
    val body = style(18, bold = false, lineHeight = 1.4f)
    val label = style(16, bold = true)
    val small = style(14, bold = false)
    val tile = style(30, bold = true, lineHeight = 1.1f)
    val stamp = style(16, bold = true, lineHeight = 1.1f).copy(letterSpacing = 0.08.em)
}

val KelimeTypography = Typography(
    displayLarge = KelimeType.display, displayMedium = KelimeType.display, displaySmall = KelimeType.title,
    headlineLarge = KelimeType.display, headlineMedium = KelimeType.title, headlineSmall = KelimeType.section,
    titleLarge = KelimeType.section, titleMedium = KelimeType.label, titleSmall = KelimeType.label,
    bodyLarge = KelimeType.body, bodyMedium = KelimeType.body, bodySmall = KelimeType.small,
    labelLarge = KelimeType.label, labelMedium = KelimeType.small, labelSmall = KelimeType.small,
)
