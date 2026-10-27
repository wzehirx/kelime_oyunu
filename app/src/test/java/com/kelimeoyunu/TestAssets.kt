package com.kelimeoyunu

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/** Gradle testte çalışma dizini app/ olur; IDE'den kök olabilir. İkisini de dener. */
object TestAssets {
    fun file(name: String): File {
        val candidates = listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Asset bulunamadı: $name (çalışma dizini ${File(".").absolutePath})")
    }

    /** Depo kökündeki dosya (ör. tools/words/blocklist.txt); Gradle testi app/ altında çalışır. */
    fun repoFile(path: String): File =
        listOf(File("../$path"), File(path)).firstOrNull { it.isFile }
            ?: error("Depo dosyası bulunamadı: $path (çalışma dizini ${File(".").absolutePath})")

    /** '#' yorumlu, satır başına bir sözcük içeren depo listesi (blocklist.txt, grid_pool.txt). */
    fun repoWordList(path: String): List<String> =
        repoFile(path).readLines(Charsets.UTF_8)
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }

    fun open(name: String): InputStream = FileInputStream(file(name))
}
