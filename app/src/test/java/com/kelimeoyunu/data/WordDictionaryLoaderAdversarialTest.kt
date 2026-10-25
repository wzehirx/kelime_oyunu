package com.kelimeoyunu.data

import com.kelimeoyunu.TestAssets
import com.kelimeoyunu.domain.TurkishText
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WordDictionaryLoaderAdversarialTest {
    private val real by lazy { loadWords { TestAssets.open("words.txt") } }

    @Test
    fun realFileLinesAreCleanAndSorted() {
        val lines = TestAssets.file("words.txt").readLines(Charsets.UTF_8)
        assertTrue("BOM var", lines.first().firstOrNull() != '﻿')
        val bad = lines.filter { it.isEmpty() || it.length !in 2..15 || it.any { c -> c !in TurkishText.ALPHABET } }
        assertTrue("${bad.size} bozuk satır: ${bad.take(10)}", bad.isEmpty())
        assertEquals("tekrar var", lines.size, lines.toSet().size)
        assertEquals(lines.size, real.size)
    }

    @Test
    fun realFileLookups() {
        for (w in listOf("kalem", "ışık", "çiçek", "ısı", "ağaç", "şeker", "gül")) assertTrue(w, real.contains(w))
        for (w in listOf("xyz", "qwx", "", "kalemm")) assertFalse(w, real.contains(w))
        assertTrue(real.contains("IŞIK") && real.contains("ÇİÇEK") && real.contains("AĞAÇ"))
    }

    @Test
    fun spellableSevenLettersIsFast() {
        real // ısıt
        val pools = listOf(
            "ıkşıyğı", "toimtce", "aeilnrt", "kalemsi", "aaeeiıınn", "çiçekler", "aaaaaaa", "zzzzzzz",
        ).map { it.take(7).toList() }
        val t0 = System.nanoTime()
        var total = 0
        repeat(25) { for (p in pools) total += real.spellable(p).size }
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("spellable: ${25 * pools.size} çağrı, $ms ms, toplam sonuç $total")
        assertTrue("200 çağrı $ms ms sürdü", ms < 10_000)
        val single = System.nanoTime().let { real.spellable("aeilnrt".toList()); (System.nanoTime() - it) / 1_000_000 }
        assertTrue("tek çağrı $single ms", single < 500)
    }

    @Test
    fun spellableResultsAreCorrect() {
        val pool = "aeilnrt".toList()
        val r = real.spellable(pool)
        assertTrue(r.isNotEmpty())
        assertTrue(r.all { it.length in 2..7 && TurkishText.canSpell(it, pool) })
        assertTrue("tarel? 'alınteri' değil ama 'liner' yok olabilir; 'terli' var olmalı", "terli" in r || "ilen" in r || "ter" in r)
        // tam tarama ile eşitlik
        val brute = TestAssets.file("words.txt").readLines().filter { it.length >= 2 && TurkishText.canSpell(it, pool) }.toSet()
        assertEquals(brute, r)
    }

    @Test
    fun emptyAndWhitespaceOnlyStreams() {
        assertEquals(0, loadWords { ByteArrayInputStream(ByteArray(0)) }.size)
        assertEquals(0, loadWords { ByteArrayInputStream("\n\n  \r\n\t\n".toByteArray()) }.size)
    }

    @Test
    fun bomAtStartDoesNotLoseFirstWord() {
        val d = loadWords { ByteArrayInputStream("﻿kalem\nelma\n".toByteArray(Charsets.UTF_8)) }
        assertTrue("BOM'lu ilk kelime kayboldu", d.contains("kalem"))
    }

    @Test
    fun streamClosedAndOpenFailurePropagatesAsIoException() {
        var closed = false
        loadWords { object : FilterInputStream(ByteArrayInputStream("a\n".toByteArray())) { override fun close() { closed = true; super.close() } } }
        assertTrue(closed)
        try {
            loadWords { throw IOException("yok") }
            fail()
        } catch (_: IOException) {
        }
    }

    @Test
    fun invalidUtf8DoesNotCrash() {
        val d = loadWords { ByteArrayInputStream(byteArrayOf(0x6B, 0x61, 0x0A, 0xFF.toByte(), 0xFE.toByte(), 0x0A, 0x65, 0x6C, 0x0A)) }
        assertTrue(d.contains("ka") && d.contains("el"))
    }
}
