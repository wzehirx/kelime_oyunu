package com.kelimeoyunu.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kelimeoyunu.a11y.initialFocus
import com.kelimeoyunu.domain.GameEconomy
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.MinLetterHeight

private val IntroParagraphs = listOf(
    "Amaç: Ekrandaki harflerle, listedeki gizli kelimeleri bulmak.",
    "Ekranın üstünde kelimelerin listesi bulunur. Her kelime için kaç harfli olduğu ve açılmış harfleri okunur.",
    "Harf düğmelerine çift dokunarak harf seçersin. Seçtiğin harflerden oluşan kelime okunur. Aynı harf düğmesini iki kez seçemezsin.",
    "Kelimeyi göndermek için Gönder düğmesine çift dokun. Sil son harfi, Temizle bütün harfleri kaldırır. Karıştır harflerin sırasını değiştirir.",
    GameEconomy.HOW_BONUS,
    GameEconomy.HOW_HINTS,
)

private const val WorldTourParagraph =
    "Dünya turu: Seviyeler 100 ülkeye bölünür ve Türkiye'den başlarsın. Yeni bir ülkeye girince bölgesine özel bir melodi çalar ve ülke bir kez tanıtılır; ${GameEconomy.HOW_WORLD_REWARD}. Ana menüdeki Dünya turu düğmesinden hangi ülkede olduğuna bakabilirsin."

private const val OutroParagraph = GameEconomy.HOW_OUTRO

/** Dünya turu kapalıyken (ülke listesi okunamadı) ana menüde böyle bir düğme olmadığından o paragraf çıkarılır. */
fun howToPlayParagraphs(worldTourEnabled: Boolean): List<String> =
    IntroParagraphs + listOfNotNull(WorldTourParagraph.takeIf { worldTourEnabled }) + OutroParagraph

val HowToPlayParagraphs: List<String> = howToPlayParagraphs(worldTourEnabled = true)

@Composable
fun HowToPlayScreen(onBack: () -> Unit, modifier: Modifier = Modifier, worldTourEnabled: Boolean = true) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Nasıl oynanır",
            Modifier.initialFocus().semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        howToPlayParagraphs(worldTourEnabled).forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        A11yButton("Geri", "Geri", onBack, Modifier.fillMaxWidth(), minHeight = MinLetterHeight)
    }
}
