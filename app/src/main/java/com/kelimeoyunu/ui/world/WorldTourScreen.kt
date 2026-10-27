package com.kelimeoyunu.ui.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.Announcements
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.domain.Place
import com.kelimeoyunu.domain.PlaceState
import com.kelimeoyunu.domain.WorldTour
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.MinLetterHeight

/**
 * Dünya turu listesi. Her satır tek TalkBack düğümüdür. Açılışta liste ve odak oyuncunun bulunduğu ülkeye
 * gelir (tüm ülkeler bitmişse başlığa). [savedLevelDone]: kayıtlı seviyenin kelimeleri tamam. Satıra dokunmak bilgiyi [onSay] ile tekrar söyletir; kilitli
 * satır "Henüz bu ülkeye ulaşmadın" der.
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
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            Announcements.placesTitle(tour.completedPlaces(currentLevelIndex), tour.placeCount),
            Modifier.then(if (current == null) Modifier.initialFocus() else Modifier).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().semantics { collectionInfo = CollectionInfo(rowCount = tour.placeCount, columnCount = 1) },
            listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tour.placeCount) { i ->
                val place = tour.place(i)!!
                PlaceRow(
                    place, tour.placeState(i, currentLevelIndex), onSay,
                    if (i == current) Modifier.initialFocus() else Modifier,
                )
            }
        }
        A11yButton("Geri", "Geri", onBack, Modifier.fillMaxWidth(), minHeight = MinLetterHeight)
    }
}

@Composable
private fun PlaceRow(place: Place, state: PlaceState, onSay: (String) -> Unit, modifier: Modifier) {
    val (description, detail, tapSays) = when (state) {
        PlaceState.Locked -> Triple(Announcements.placeLocked(place), "Kilitli", Announcements.placeNotReached())
        is PlaceState.InProgress -> Announcements.placeInProgress(place, state.done, state.total).let {
            Triple(it, "${state.done} / ${state.total} seviye, şu an buradasın", it)
        }
        is PlaceState.Completed -> Announcements.placeCompletedRow(place).let {
            Triple(it, "Tamamlandı. Başkenti ${place.baskent}. ${place.bilgi1} ${place.bilgi2}", it)
        }
    }
    Column(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = { onSay(tapSays) })
            .semantics { contentDescription = description; role = Role.Button }
            .padding(vertical = 12.dp),
    ) {
        Text(place.ad, Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.titleMedium)
        Text(detail, Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.bodyMedium)
    }
}
