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
import com.kelimeoyunu.ui.common.A11yButton
import com.kelimeoyunu.ui.common.MinLetterHeight

val HowToPlayParagraphs = listOf(
    "Amaç: Ekrandaki harflerle, listedeki gizli kelimeleri bulmak.",
    "Ekranın üstünde kelimelerin listesi bulunur. Her kelime için kaç harfli olduğu ve açılmış harfleri okunur.",
    "Harf düğmelerine çift dokunarak harf seçersin. Seçtiğin harflerden oluşan kelime okunur. Aynı harf düğmesini iki kez seçemezsin.",
    "Kelimeyi göndermek için Gönder düğmesine çift dokun. Sil son harfi, Temizle bütün harfleri kaldırır. Karıştır harflerin sırasını değiştirir.",
    "Listede olmayan ama geçerli bir kelime bulursan bonus kelime olur. Her 5 bonus kelime 20 jeton kazandırır. Seviyeyi bitirince 10 jeton kazanırsın.",
    "Takılırsan İpucu düğmesi 10 jetonla bir harf açar. Kelime aç düğmesi 30 jetonla bir kelimeyi tamamen açar.",
    "Dünya turu: Seviyeler 100 ülkeye bölünür ve Türkiye'den başlarsın. Yeni bir ülkeye girince bölgesine özel bir melodi çalar ve ülke tanıtılır; ülkeyi bitirince 25 jeton ve ilginç bir bilgi kazanırsın. Ana menüdeki Dünya turu düğmesinden hangi ülkede olduğuna bakabilirsin.",
    "Her şey sesli olarak duyurulur; ekrana bakman gerekmez. Başlangıçta 50 jetonun var.",
)

@Composable
fun HowToPlayScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Nasıl oynanır",
            Modifier.initialFocus().semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        HowToPlayParagraphs.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        A11yButton("Geri", "Geri", onBack, Modifier.fillMaxWidth(), minHeight = MinLetterHeight)
    }
}
