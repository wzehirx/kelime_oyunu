package com.kelimeoyunu

import com.kelimeoyunu.domain.Place

/** Testler için sahte yer listesi: ilki Türkiye, geri kalanı "Ülke N". */
object TestPlaces {
    val turkey = Place(
        "tr", "Türkiye", "Asya", "ortadogu", "Ankara",
        "Türkiye iki kıtada yer alır.", "Kapadokya'da peri bacaları vardır.",
    )

    fun list(n: Int, region: (Int) -> String = { "avrupa" }): List<Place> = List(n) { i ->
        if (i == 0) turkey else Place("p$i", "Ülke $i", "Avrupa", region(i), "Başkent $i", "Bilgi bir $i.", "Bilgi iki $i.")
    }
}
