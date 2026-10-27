package com.kelimeoyunu.domain

/**
 * Jeton ekonomisi: tüm ödül ve maliyet sabitleri ile bunlardan türeyen kullanıcı metinleri tek yerde.
 * Ekran metinleri (düğme etiketleri, "Nasıl oynanır") buradaki sabitlerden üretilir; sayı değişirse
 * metinler kendiliğinden güncellenir ve tutarsızlık olmaz.
 */
object GameEconomy {
    const val START_COINS = 50

    /** Bu kadar bonus kelime bulununca sayaç dolar ve [BONUS_REWARD] verilir. */
    const val BONUS_TARGET = 5
    const val BONUS_REWARD = 10
    const val LEVEL_REWARD = 5
    const val PLACE_REWARD = 25
    const val HINT_LETTER_COST = 15
    const val HINT_WORD_COST = 40

    /** Erişilebilirlik açıklaması ve görünen etiket (ActionBar). */
    const val HINT_LETTER_DESCRIPTION = "İpucu, harf aç, $HINT_LETTER_COST jeton"
    const val HINT_LETTER_LABEL = "İpucu: harf aç, $HINT_LETTER_COST jeton"
    const val HINT_WORD_DESCRIPTION = "Kelime aç, $HINT_WORD_COST jeton"
    const val HINT_WORD_LABEL = "Kelime aç, $HINT_WORD_COST jeton"

    /** "Nasıl oynanır" paragrafları. */
    const val HOW_BONUS =
        "Listede olmayan ama geçerli bir kelime bulursan bonus kelime olur. Her $BONUS_TARGET bonus kelime " +
            "$BONUS_REWARD jeton kazandırır. Seviyeyi bitirince $LEVEL_REWARD jeton kazanırsın."
    const val HOW_HINTS =
        "Takılırsan İpucu düğmesi $HINT_LETTER_COST jetonla bir harf açar. Kelime aç düğmesi $HINT_WORD_COST " +
            "jetonla bir kelimeyi tamamen açar."
    const val HOW_WORLD_REWARD = "ülkeyi bitirince $PLACE_REWARD jeton ve ilginç bir bilgi kazanırsın"
    const val HOW_OUTRO =
        "Her şey sesli olarak duyurulur; ekrana bakman gerekmez. Başlangıçta $START_COINS jetonun var."
}
