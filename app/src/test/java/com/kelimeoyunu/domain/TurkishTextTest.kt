package com.kelimeoyunu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkishTextTest {
    @Test
    fun lowerHandlesDottedAndDotlessI() {
        assertEquals("ışık", TurkishText.lower("IŞIK"))
        assertEquals("izmir", TurkishText.lower("İZMİR"))
        assertEquals("ısı", TurkishText.lower("ISI"))
        assertEquals("isi", TurkishText.lower("İSİ"))
    }

    @Test
    fun upperHandlesDottedAndDotlessI() {
        assertEquals("IŞIK", TurkishText.upper("ışık"))
        assertEquals("İZMİR", TurkishText.upper("izmir"))
        assertEquals("ÇĞÖŞÜ", TurkishText.upper("çğöşü"))
    }

    @Test
    fun alphabetHas29Letters() {
        assertEquals("abcçdefgğhıijklmnoöprsştuüvyz", TurkishText.ALPHABET)
        assertEquals(29, TurkishText.ALPHABET.length)
    }

    @Test
    fun canSpellRespectsLetterMultiplicity() {
        assertFalse(TurkishText.canSpell("ala", listOf('a', 'l')))
        assertTrue(TurkishText.canSpell("ala", listOf('a', 'l', 'a')))
        assertTrue(TurkishText.canSpell("ala", listOf('l', 'a', 'a', 'x')))
        assertFalse(TurkishText.canSpell("ala-ala", listOf('a', 'l', 'a')))
        assertFalse(TurkishText.canSpell("ala", listOf('a', 'l', 'ğ')))
    }

    @Test
    fun canSpellDistinguishesDottedI() {
        assertTrue(TurkishText.canSpell("ısı", listOf('ı', 's', 'ı')))
        assertFalse(TurkishText.canSpell("isi", listOf('ı', 's', 'ı')))
    }

    @Test
    fun canSpellEmptyWordIsFalse() {
        assertFalse(TurkishText.canSpell("", listOf('a')))
    }
}
