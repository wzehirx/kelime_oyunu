package com.kelimeoyunu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDictionaryAdversarialTest {
    @Test
    fun isiVsIsiInDictionary() {
        val d = WordDictionary.fromLines(sequenceOf("ISI"))      // -> ısı
        assertTrue(d.contains("ısı"))
        assertTrue(d.contains("ISI"))
        assertFalse(d.contains("isi"))
        assertFalse(d.contains("İSİ"))
        val d2 = WordDictionary.fromLines(sequenceOf("İSİ"))     // -> isi
        assertTrue(d2.contains("isi"))
        assertTrue(d2.contains("İSİ"))
        assertFalse(d2.contains("ısı"))
        assertFalse(d2.contains("ISI"))
    }

    @Test
    fun bothInDictionaryAreSeparate() {
        val d = WordDictionary.fromLines(sequenceOf("ısı", "isi"))
        assertEquals(2, d.size)
        assertTrue(d.contains("ISI") && d.contains("İSİ"))
        assertEquals(setOf("ısı"), d.spellable(listOf('ı', 's', 'ı')))
        assertEquals(setOf("isi"), d.spellable(listOf('i', 's', 'i')))
        assertEquals(emptySet<String>(), d.spellable(listOf('ı', 's', 'i')))
    }

    @Test
    fun fromLinesDedupesAcrossCaseAndWhitespace() {
        val d = WordDictionary.fromLines(sequenceOf("Kalem", "KALEM", " kalem\r", "kalem\t", "\u00A0"))
        assertEquals(1, d.size)
    }

    @Test
    fun spellableNeverReturnsTooShortOrUnspellable() {
        val d = WordDictionary(setOf("a", "al", "ala", "alaa", "lal", "x", ""))
        val r = d.spellable(listOf('a', 'l', 'a'))
        assertEquals(setOf("al", "ala"), r)
        assertTrue(d.spellable(listOf('a', 'l', 'a'), min = 100).isEmpty())
        assertTrue(d.spellable(emptyList()).isEmpty())
        assertTrue(d.spellable(emptyList(), min = 0).isEmpty()) // boş kelime yazılamaz
    }

    @Test
    fun containsToleratesWeirdInput() {
        val d = WordDictionary(setOf("kalem"))
        assertFalse(d.contains("   "))
        assertFalse(d.contains("kal em"))
        assertTrue(d.contains("\tKalem\n"))
    }
}
