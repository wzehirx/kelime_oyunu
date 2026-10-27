package com.kelimeoyunu.ui.world

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.domain.Place
import com.kelimeoyunu.domain.PlaceState
import com.kelimeoyunu.domain.WorldTour
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.ButtonKind
import com.kelimeoyunu.ui.common.IconKind
import com.kelimeoyunu.ui.common.KIcon
import com.kelimeoyunu.ui.common.ListRow
import com.kelimeoyunu.ui.common.MinLetterHeight
import com.kelimeoyunu.ui.common.ProgressLine
import com.kelimeoyunu.ui.common.ProgressRing
import com.kelimeoyunu.ui.common.Stamp
import com.kelimeoyunu.ui.common.placeInitials
import com.kelimeoyunu.ui.theme.KelimeType
import com.kelimeoyunu.ui.theme.Radius
import com.kelimeoyunu.ui.theme.Space
import com.kelimeoyunu.ui.theme.kelimeColors

/**
 * Dünya turu listesi. Her satır tek TalkBack düğümüdür. Açılışta liste ve odak oyuncunun bulunduğu ülkeye
 * gelir (tüm ülkeler bitmişse başlığa). [savedLevelDone]: kayıtlı seviyenin kelimeleri tamam. Satıra dokunmak bilgiyi [onSay] ile tekrar söyletir; kilitli
 * satır "Henüz bu ülkeye ulaşmadın" der. Görsel: başlık + halka, çizgili defter satırları, bölge damgası ve
 * durum hapı (metin + simge). Tamamlanan ülkenin bilgileri satıra dokunulunca görsel olarak açılır.
 */
@Composable
fun WorldTourScreen(
    tour: WorldTour,
    savedLevelIndex: Int,
    onSay: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    savedLevelDone: Boolean = false,
) {
    // Kayıtlı seviye bitmiş ama "Sonraki seviye"ye basılmamışsa o seviye ilerlemeye sayılır (ülke bitmiş görünür).
    val currentLevelIndex = savedLevelIndex + if (savedLevelDone) 1 else 0
    // Odak ve kaydırma, "Devam et" ile aynı ülkede kalır (seviye bitmiş olsa da basılana kadar oradasın).
    val current = tour.positionOf(savedLevelIndex)?.placeIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = current ?: 0)
    val completed = tour.completedPlaces(currentLevelIndex)
    val k = kelimeColors
    Column(
        modifier.fillMaxSize().padding(horizontal = Space.md, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Text(
                Announcements.placesTitle(completed, tour.placeCount),
                Modifier.weight(1f).then(if (current == null) Modifier.initialFocus() else Modifier).semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = k.ink,
            )
            ProgressRing(if (tour.placeCount > 0) completed.toFloat() / tour.placeCount else 0f, k.primary, size = 44.dp)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().semantics { collectionInfo = CollectionInfo(rowCount = tour.placeCount, columnCount = 1) },
            listState,
        ) {
            items(tour.placeCount) { i ->
                val place = tour.place(i)!!
                PlaceRow(
                    place, tour.placeState(i, currentLevelIndex), onSay,
                    if (i == current) Modifier.initialFocus() else Modifier,
                    isCurrent = i == current,
                )
            }
        }
        A11yButton("Geri", "Geri", onBack, Modifier.fillMaxWidth(), minHeight = MinLetterHeight, kind = ButtonKind.Secondary)
    }
}

@Composable
private fun PlaceRow(place: Place, state: PlaceState, onSay: (String) -> Unit, modifier: Modifier, isCurrent: Boolean) {
    val k = kelimeColors
    val (description, detail, tapSays) = when (state) {
        PlaceState.Locked -> Triple(Announcements.placeLocked(place), "Kilitli", Announcements.placeNotReached())
        is PlaceState.InProgress -> Announcements.placeInProgress(place, state.done, state.total).let {
            Triple(it, "${state.done} / ${state.total} seviye, şu an buradasın", it)
        }
        is PlaceState.Completed -> Announcements.placeCompletedRow(place).let {
            Triple(it, "Tamamlandı. Başkenti ${place.baskent}. ${place.bilgi1} ${place.bilgi2}", it)
        }
    }
    val ink = k.regionInk(place.bolge)
    // Tamamlanan ülkenin bilgileri açılınca görünür (yalnızca görsel; ekran okuyucu zaten tam metni okur).
    var expanded by rememberSaveable(place.id) { mutableStateOf(false) }
    ListRow(
        description = description,
        onClick = {
            if (state is PlaceState.Completed) expanded = !expanded
            onSay(tapSays)
        },
        modifier = modifier,
        highlight = isCurrent && state is PlaceState.InProgress,
        leading = { Stamp(placeInitials(place.ad), ink, locked = state is PlaceState.Locked) },
    ) {
        Text(place.ad, style = KelimeType.section, color = if (state is PlaceState.Locked) k.inkMuted else k.ink)
        StatusPill(state)
        when (state) {
            is PlaceState.InProgress -> {
                Text("${state.done} / ${state.total} seviye", style = KelimeType.small, color = k.inkMuted)
                ProgressLine(state.done.toFloat() / state.total, ink, Modifier.padding(top = 4.dp))
            }
            is PlaceState.Completed -> {
                ProgressLine(1f, ink, Modifier.padding(top = 4.dp))
                if (expanded) Text(detail.removePrefix("Tamamlandı. "), style = KelimeType.small, color = k.ink)
            }
            PlaceState.Locked -> Unit
        }
    }
}

/** Durum hapı: metin + simge (renk tek başına bilgi taşımaz). */
@Composable
private fun StatusPill(state: PlaceState) {
    val k = kelimeColors
    val (text, icon, tint) = when (state) {
        PlaceState.Locked -> Triple("Kilitli", IconKind.Lock, k.inkMuted)
        is PlaceState.InProgress -> Triple("Devam", IconKind.Play, k.primary)
        is PlaceState.Completed -> Triple("Tamamlandı", IconKind.Check, k.success)
    }
    Surface(shape = RoundedCornerShape(Radius.pill), color = k.surface, border = BorderStroke(1.5.dp, tint)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KIcon(icon, tint, size = 16.dp)
            Text(text, style = KelimeType.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = k.ink, maxLines = 1)
        }
    }
}
