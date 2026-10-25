package com.kelimeoyunu.data

import com.kelimeoyunu.domain.Level
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * gzip + UTF-8 seviye dosyasını okur. Bozuk satırlar atlanır; eksik, boş ya da bozuk akış
 * çökmez (okunabilen seviyeler kalır, hiç yoksa count = 0).
 */
class LevelRepository(private val open: () -> InputStream) {
    private val levels: List<Level> by lazy { load() }

    val count: Int get() = levels.size

    /** 0 tabanlı sıra; aralık dışı için null. Level.id dosyadaki id'dir. */
    fun get(index: Int): Level? = levels.getOrNull(index)

    private fun load(): List<Level> {
        val result = ArrayList<Level>()
        try {
            open().use { raw ->
                GZIPInputStream(raw).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    for (line in lines) LevelParser.parse(line)?.let { result += it }
                }
            }
        } catch (_: IOException) {
            // Eksik/bozuk asset: o ana kadar okunanlarla devam.
        }
        return result
    }
}
