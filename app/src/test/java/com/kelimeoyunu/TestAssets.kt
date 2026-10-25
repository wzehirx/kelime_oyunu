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

    fun open(name: String): InputStream = FileInputStream(file(name))
}
