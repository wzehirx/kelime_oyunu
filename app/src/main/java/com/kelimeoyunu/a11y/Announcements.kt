package com.kelimeoyunu.a11y

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

    fun headerDescription(levelNumber: Int, coins: Int, bonusMeter: Int, target: Int, remainingWords: Int): String =
        "Seviye $levelNumber. $coins jeton. Kalan kelime: $remainingWords. Bonus sayacı: $bonusMeter / $target"
}
