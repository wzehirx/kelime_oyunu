package com.kelimeoyunu.a11y

import com.kelimeoyunu.domain.Place
import com.kelimeoyunu.domain.PlacePosition
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.ui.game.WordRow

/** Tüm Türkçe duyuru ve erişilebilirlik metinleri tek yerde; saf Kotlin, birim testlidir. */
object Announcements {
    private fun up(s: String) = TurkishText.upper(s)

    fun letterAdded(c: Char): String = up(c.toString())

    fun deleted(remaining: String): String =
        if (remaining.isEmpty()) "Kelime boş" else "Silindi, kalan: ${up(remaining)}"

    fun cleared(): String = "Temizlendi"

    fun result(r: SubmitResult): String = when (r) {
        is SubmitResult.Correct -> "Doğru, ${up(r.word)}"
        is SubmitResult.Bonus -> "Bonus kelime, ${up(r.word)}"
        is SubmitResult.AlreadyFound -> "Bu kelime zaten bulundu"
        is SubmitResult.Invalid -> "Geçersiz kelime"
        SubmitResult.Empty -> "Önce harf seç"
    }

    fun shuffled(letters: List<Char>): String =
        "Harfler karıştırıldı: " + letters.joinToString(", ") { up(it.toString()) }

    fun hintUsed(coins: Int): String = "İpucu kullanıldı, kalan jeton: $coins"
    fun notEnoughCoins(): String = "Yeterli jeton yok"
    fun noHint(): String = "Açılacak ipucu yok"
    fun coinsEarned(n: Int): String = "$n jeton kazandın"
    fun bonusMeterFull(): String = "Bonus sayacı doldu"
    fun levelCompleted(n: Int): String = "Seviye $n tamamlandı"
    fun allDone(): String = "Tebrikler, tüm seviyeleri tamamladın"
    fun progressReset(): String = "İlerleme sıfırlandı"

    fun wordRowDescription(row: WordRow): String {
        val head = "Kelime ${row.number}, ${row.length} harfli"
        return if (row.found) {
            "$head, bulundu: ${up(row.cells.joinToString("") { it?.toString().orEmpty() })}"
        } else {
            "$head, açık harfler: " + row.cells.joinToString(" ") { it?.let { c -> up(c.toString()) } ?: "_" }
        }
    }

    fun currentWordDescription(word: String): String =
        "Oluşturulan kelime: " + if (word.isEmpty()) "boş" else up(word)

    fun letterButton(c: Char, selected: Boolean): String =
        "Harf ${up(c.toString())}" + if (selected) ", seçildi" else ""

    fun headerDescription(
        levelNumber: Int, coins: Int, bonusMeter: Int, target: Int, remainingWords: Int,
        place: PlacePosition? = null,
    ): String {
        val head = if (place == null) "Seviye $levelNumber" else "Seviye $levelNumber, ${placeProgress(place)}"
        return "$head. $coins jeton. Kalan kelime: $remainingWords. Bonus sayacı: $bonusMeter / $target"
    }

    /** "Türkiye 3 / 20" */
    fun placeProgress(p: PlacePosition): String = "${p.place.ad} ${p.levelInPlace} / ${p.placeLevelCount}"

    fun continueLabel(place: PlacePosition): String =
        "Devam et, ${place.place.ad}, ${place.levelInPlace} / ${place.placeLevelCount}"

    fun placeEntered(place: Place, levelCount: Int): String =
        "Yeni ülke: ${place.ad}, ${place.kita}. Başkenti ${place.baskent}. " +
            "$levelCount seviyelik yolculuk başlıyor. Biliyor muydun? ${place.bilgi1}"

    fun placeCompleted(place: Place, bonus: Int, next: String?): String =
        "${place.ad} tamamlandı! $bonus jeton kazandın. Biliyor muydun? ${place.bilgi2}" +
            if (next != null) " Sıradaki ülke: $next." else ""

    fun placesTitle(completed: Int, total: Int): String = "Dünya turu, $completed / $total ülke tamamlandı"
    fun placeLocked(place: Place): String = "${place.ad}, kilitli"
    fun placeInProgress(place: Place, done: Int, total: Int): String =
        "${place.ad}, $done / $total seviye, şu an buradasın"
    fun placeCompletedRow(place: Place): String =
        "${place.ad}, tamamlandı. Başkenti ${place.baskent}. ${place.bilgi1} ${place.bilgi2}"
    fun placeNotReached(): String = "Henüz bu ülkeye ulaşmadın"
}
