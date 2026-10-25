package com.kelimeoyunu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDictionaryTest {
    private val dict = WordDictionary.fromLines(
        sequenceOf("kalem", "ışık", "ısı", "isi", "ala", "al", "a", "", "  lam  ", "İZMİR")
    )

    @Test
    fun containsIsCaseInsensitiveWithTurkishRules() {
        assertTrue(dict.contains("kalem"))
        assertTrue(dict.contains("KALEM"))
        assertTrue(dict.contains("IŞIK"))
        assertTrue(dict.contains("Işık"))
        assertTrue(dict.contains("izmir"))
        assertTrue(dict.contains("İzmir"))
        assertFalse(dict.contains("xyz"))
        assertFalse(dict.contains(""))
    }

    @Test
    fun isiAndIsiAreDifferentWords() {
        val onlyDotless = WordDictionary(setOf("ısı"))
        assertTrue(onlyDotless.contains("ISI"))
        assertFalse(onlyDotless.contains("isi"))
        assertFalse(onlyDotless.contains("İSİ"))
    }

    @Test
    fun fromLinesTrimsAndSkipsBlank() {
        assertTrue(dict.contains("lam"))
        assertFalse(dict.contains(" "))
    }

    @Test
    fun spellableUsesMultisetAndMinLength() {
        assertEquals(setOf("ala", "al"), dict.spellable(listOf('a', 'l', 'a')))
        assertEquals(setOf("al"), dict.spellable(listOf('a', 'l')))
        assertEquals(setOf("ala", "al", "a"), dict.spellable(listOf('a', 'l', 'a'), min = 1))
        assertEquals(setOf("ala"), dict.spellable(listOf('a', 'l', 'a'), min = 3))
    }
}
