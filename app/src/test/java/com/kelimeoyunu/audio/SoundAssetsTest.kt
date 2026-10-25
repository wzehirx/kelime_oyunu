package com.kelimeoyunu.audio

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundAssetsTest {
    private val dir = listOf("src/main/assets/sounds", "app/src/main/assets/sounds").map(::File).first { it.isDirectory }

    @Test fun everySfxHasValidMono44kWav() {
        for (sfx in Sfx.entries) {
            val f = File(dir, sfx.fileName)
            assertTrue("eksik: ${f.path}", f.isFile)
            val b = ByteBuffer.wrap(f.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            assertTrue("kısa: $sfx", b.limit() > 44)
            fun tag(o: Int) = String(ByteArray(4) { b.get(o + it) }, Charsets.US_ASCII)
            assertEquals("RIFF", tag(0)); assertEquals("WAVE", tag(8)); assertEquals("fmt ", tag(12))
            assertEquals("$sfx PCM", 1, b.getShort(20).toInt())
            assertEquals("$sfx mono", 1, b.getShort(22).toInt())
            assertEquals("$sfx rate", 44100, b.getInt(24))
        }
    }
}
