package com.kelimeoyunu.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.text
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.domain.PlacePosition
import com.kelimeoyunu.ui.common.CappedFontScale
import com.kelimeoyunu.ui.common.IconBadge
import com.kelimeoyunu.ui.common.IconKind
import com.kelimeoyunu.ui.common.HeroPanel
import com.kelimeoyunu.ui.common.ListRow
import com.kelimeoyunu.ui.common.ProgressRing
import com.kelimeoyunu.ui.common.Stamp
import com.kelimeoyunu.ui.common.placeInitials
import com.kelimeoyunu.ui.theme.Elevation
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.Space
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * Ana menü. Başlangıç odağı "Devam et" panelindedir ([focusSettings] ise ayarlardan dönüldüğü için
 * "Ayarlar"da, [focusWorld] ise Dünya turu ekranından dönüldüğü için "Dünya turu"nda). [levelNumber] null ise
 * ilerleme henüz okunmadı; [allDone] ise tüm seviyeler bitmiştir. [place] varsa "Devam et" ülkeyi söyler;
 * [onWorldTour] null ise (tur kapalı) "Dünya turu" satırı gösterilmez.
 *
 * Görsel: karolardan logo, "Devam et" kahraman paneli, alt çizgili ikincil satırlar ve altbilgi. Her panel/satır
 * TEK anlamsal düğümdür (etiketler değişmedi); logo tek başlık, altbilgi ([coins], [totalLevels]) süs olarak gizlidir.
 */
@Composable
fun MainMenuScreen(
    levelNumber: Int?,
    onContinue: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    allDone: Boolean = false,
    focusSettings: Boolean = false,
    place: PlacePosition? = null,
    onWorldTour: (() -> Unit)? = null,
    focusWorld: Boolean = false,
    coins: Int? = null,
    totalLevels: Int? = null,
    worldCompleted: Int? = null,
    worldTotal: Int? = null,
) {
    val k = kelimeColors
    val continueLabel = when {
        allDone -> "Devam et, tüm seviyeler tamamlandı"
        levelNumber == null -> "Devam et"
        place != null -> Announcements.continueLabel(place)
        else -> "Devam et, Seviye $levelNumber"
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.md, vertical = Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        TileLogo()
        val ink = k.regionInk(place?.place?.bolge)
        HeroPanel(
            description = continueLabel,
            actionLabel = "Devam et",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().then(if (focusSettings || focusWorld) Modifier else Modifier.initialFocus()),
            title = when {
                allDone -> "Tüm seviyeler tamamlandı"
                levelNumber == null -> "Yükleniyor"
                else -> "Seviye $levelNumber"
            },
            subtitle = if (allDone) null else place?.place?.ad,
            progress = if (allDone || place == null) null else place.levelInPlace.toFloat() / place.placeLevelCount,
            progressColor = ink,
            leading = {
                if (place != null && !allDone) Stamp(placeInitials(place.place.ad), ink)
                else IconBadge(IconKind.Compass)
            },
        )
        Column {
            if (onWorldTour != null) {
                ListRow(
                    description = "Dünya turu",
                    onClick = onWorldTour,
                    modifier = if (focusWorld) Modifier.initialFocus() else Modifier,
                    leading = { IconBadge(IconKind.Globe) },
                    trailing = {
                        if (worldCompleted != null && worldTotal != null && worldTotal > 0) {
                            ProgressRing(worldCompleted.toFloat() / worldTotal, k.primary)
                        }
                    },
                ) {
                    Text("Dünya turu", style = KelimeType.section, color = k.ink)
                    if (worldCompleted != null && worldTotal != null) {
                        Text("$worldCompleted / $worldTotal ülke", style = KelimeType.small, color = k.inkMuted)
                    }
                }
            }
            ListRow("Nasıl oynanır", onHowToPlay, leading = { IconBadge(IconKind.Book) }) {
                Text("Nasıl oynanır", style = KelimeType.section, color = k.ink)
            }
            ListRow(
                "Ayarlar", onSettings,
                modifier = if (focusSettings) Modifier.initialFocus() else Modifier,
                leading = { IconBadge(IconKind.Sliders) },
            ) {
                Text("Ayarlar", style = KelimeType.section, color = k.ink)
            }
        }
        if (coins != null || totalLevels != null) {
            val parts = listOfNotNull(coins?.let { "Jeton: $it" }, totalLevels?.let { "Toplam seviye: $it" })
            Text(
                parts.joinToString("   "),
                Modifier.fillMaxWidth().clearAndSetSemantics {},
                style = KelimeType.small, color = k.inkMuted, textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * "KELİME OYUNU" işareti: iki satır fildişi karo. Ekran okuyucuya tek başlık ("Kelime Oyunu") olarak görünür;
 * karolar süstür. Karo boyutu satır genişliğine göre küçülür (en çok 44 dp).
 */
@Composable
private fun TileLogo() {
    val k = kelimeColors
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics {
            heading()
            text = AnnotatedString("Kelime Oyunu")
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CappedFontScale(1.15f) {
            listOf("KELİME", "OYUNU").forEach { word ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    word.forEach { ch ->
                        Box(
                            Modifier.weight(1f, fill = false).widthIn(max = 44.dp).aspectRatio(1f)
                                .shadow(Elevation.token, RoundedCornerShape(8.dp))
                                .background(k.tile, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(ch.toString(), style = KelimeType.section, color = k.tileInk, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
