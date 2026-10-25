package com.kelimeoyunu.data

import com.kelimeoyunu.domain.Place
import java.io.InputStream
import org.json.JSONArray

private val REGIONS = setOf("avrupa", "asya", "ortadogu", "afrika", "amerika", "okyanusya")

/**
 * places.json'u okur. Dosya eksik, bozuk ya da herhangi bir kayıt hatalıysa (alan yok, boş alan,
 * bilinmeyen bölge) boş liste döner; sıra ilerlemeyi belirlediği için kısmi liste kullanılmaz.
 */
fun loadPlaces(open: () -> InputStream): List<Place> =
    try {
        val array = JSONArray(open().use { it.readBytes().toString(Charsets.UTF_8) })
        val places = ArrayList<Place>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            fun field(name: String) = o.getString(name).trim().also { require(it.isNotEmpty()) { name } }
            val bolge = field("bolge")
            require(bolge in REGIONS) { bolge }
            places += Place(
                field("id"), field("ad"), field("kita"), bolge, field("baskent"), field("bilgi1"), field("bilgi2"),
            )
        }
        places
    } catch (_: Exception) {
        emptyList()
    }
