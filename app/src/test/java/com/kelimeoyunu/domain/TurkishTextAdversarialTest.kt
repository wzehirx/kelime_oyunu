package com.kelimeoyunu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkishTextAdversarialTest {
    @Test
    fun dottedDotlessMatrix() {
        assertEquals("ısı", TurkishText.lower("ISI"))
        assertEquals("isi", TurkishText.lower("İSİ"))
        assertEquals("isi", TurkishText.lower("isi"))
        assertEquals("ısı", TurkishText.lower("ısı"))
        assertEquals("ıi", TurkishText.lower("Iİ"))
        assertEquals("ıi", TurkishText.lower("ıi"))
        assertNotEquals(TurkishText.lower("ISI"), TurkishText.lower("İSİ"))
        assertEquals("I", TurkishText.upper("ı"))
        assertEquals("İ", TurkishText.upper("i"))
    }

    @Test
    fun everyAlphabetLetterRoundTripsAndIsSingleChar() {
        for (c in TurkishText.ALPHABET) {
            val up = TurkishText.upper(c.toString())
            assertEquals("upper($c) tek karakter olmalı", 1, up.length)
            assertEquals(c.toString(), TurkishText.lower(up))
        }
    }

    @Test
    fun alphabetHasNoDuplicatesAndExcludesQwx() {
        assertEquals(29, TurkishText.ALPHABET.toSet().size)
        assertFalse(TurkishText.ALPHABET.any { it in "qwx" })
    }

    @Test
    fun canSpellMultiplicityMatrix() {
        assertFalse(TurkishText.canSpell("ala", listOf('a', 'l')))
        assertFalse(TurkishText.canSpell("aa", listOf('a')))
        assertTrue(TurkishText.canSpell("a", listOf('a')))
        assertFalse(TurkishText.canSpell("alaa", listOf('a', 'l', 'a')))
        assertTrue(TurkishText.canSpell("ala", listOf('a', 'l', 'a')))
        assertTrue(TurkishText.canSpell("lala", listOf('a', 'l', 'a', 'l')))
        assertFalse(TurkishText.canSpell("lala", listOf('a', 'l', 'a')))
        assertFalse(TurkishText.canSpell("a", emptyList()))
        assertFalse(TurkishText.canSpell("", emptyList()))
    }

    @Test
    fun canSpellIsCaseInsensitiveBothSides() {
        assertTrue(TurkishText.canSpell("ALA", listOf('a', 'l', 'a')))
        assertTrue(TurkishText.canSpell("ala", listOf('A', 'L', 'A')))
        assertFalse(TurkishText.canSpell("ALA", listOf('A', 'L')))
    }

    @Test
    fun canSpellUppercaseIPoolRules() {
        // 'I' havuz harfi ı demektir, 'İ' i demektir
        assertTrue(TurkishText.canSpell("ısı", listOf('I', 'S', 'I')))
        assertFalse(TurkishText.canSpell("isi", listOf('I', 'S', 'I')))
        assertTrue(TurkishText.canSpell("isi", listOf('İ', 'S', 'İ')))
        assertFalse(TurkishText.canSpell("ısı", listOf('İ', 'S', 'İ')))
        assertTrue(TurkishText.canSpell("ISI", listOf('ı', 's', 'ı')))
        assertFalse(TurkishText.canSpell("İSİ", listOf('ı', 's', 'ı')))
        assertFalse(TurkishText.canSpell("ısi", listOf('ı', 's', 'ı')))
    }

    @Test
    fun canSpellDoesNotMutateOrDependOnPoolOrder() {
        val pool = listOf('k', 'a', 'l', 'e', 'm')
        val copy = pool.toList()
        assertTrue(TurkishText.canSpell("kalem", pool))
        assertTrue(TurkishText.canSpell("kalem", pool))
        assertEquals(copy, pool)
        assertTrue(TurkishText.canSpell("kalem", pool.reversed()))
    }

    @Test
    fun specialCharsAreNeverSpellable() {
        assertFalse(TurkishText.canSpell("a b", listOf('a', 'b')))
        assertFalse(TurkishText.canSpell("ala-", listOf('a', 'l', 'a')))
        assertFalse(TurkishText.canSpell("ala\n", listOf('a', 'l', 'a')))
    }

    @Test
    fun turkishSpecificLettersAreDistinct() {
        assertFalse(TurkishText.canSpell("gül", listOf('g', 'u', 'l')))
        assertFalse(TurkishText.canSpell("sac", listOf('s', 'a', 'ç')))
        assertTrue(TurkishText.canSpell("çağ", listOf('ğ', 'a', 'ç')))
    }
}
