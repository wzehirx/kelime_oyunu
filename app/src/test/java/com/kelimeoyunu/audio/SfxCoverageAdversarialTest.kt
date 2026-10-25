package com.kelimeoyunu.audio

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec bölüm 6: listelenen her olay için ses var ve gerçekten bir yerden çalınıyor. */
class SfxCoverageAdversarialTest {
    private fun mainSources(): List<File> {
        val root = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
        return root.walkTopDown().filter { it.extension == "kt" }.toList()
    }

    @Test fun everyShippedSoundIsReferencedByProductionCode() {
        val text = mainSources().joinToString("\n") { it.readText() }
        val unused = Sfx.entries.filter { !text.contains("Sfx.${it.name}") }
        assertTrue("hiçbir yerden çalınmayan ses(ler) (örn. 'menü gezinme' NAV): $unused", unused.isEmpty())
    }

    @Test fun everySfxHasAWavInAssets() {
        val dir = listOf(File("src/main/assets/sounds"), File("app/src/main/assets/sounds")).first { it.isDirectory }
        Sfx.entries.forEach { assertTrue("eksik: ${it.fileName}", File(dir, it.fileName).length() > 44) }
    }
}
