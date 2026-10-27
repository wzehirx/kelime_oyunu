package com.kelimeoyunu.data

import com.kelimeoyunu.TestAssets
import java.io.ByteArrayInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDictionaryLoaderTest {
    @Test
    fun loadsWordsFromStream() {
        val d = loadWords { ByteArrayInputStream("kalem\nışık\n\nİzmir\r\n".toByteArray(Charsets.UTF_8)) }
        assertTrue(d.contains("KALEM"))
        assertTrue(d.contains("IŞIK"))
        assertTrue(d.contains("izmir"))
        assertFalse(d.contains("xyz"))
    }

    @Test
    fun realWordsFile() {
        val d = loadWords { TestAssets.open("words.txt") }
        assertTrue(d.contains("kalem"))
        assertTrue(d.contains("ışık"))
        assertTrue(d.contains("IŞIK"))
        assertFalse(d.contains("xyz"))
        assertTrue("kabul seti en az 50.000 kelime: ${d.size}", d.size >= 50_000)
    }
}
