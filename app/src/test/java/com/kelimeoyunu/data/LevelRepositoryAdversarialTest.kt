package com.kelimeoyunu.data

import com.kelimeoyunu.TestAssets
import com.kelimeoyunu.domain.CellState
import com.kelimeoyunu.domain.GameEngine
import com.kelimeoyunu.domain.Level
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.domain.WordDictionary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRepositoryAdversarialTest {
    private fun gzBytes(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }
    private fun gz(text: String) = gzBytes(text.toByteArray(Charsets.UTF_8))
    private fun repo(text: String) = LevelRepository { ByteArrayInputStream(gz(text)) }

    private val real by lazy { LevelRepository { TestAssets.open("levels.txt.gz") } }
    private val realDict by lazy { loadWords { TestAssets.open("words.txt") } }
    private val allLevels: List<Level> by lazy { (0 until real.count).map { real.get(it)!! } }

    // ---------- gerçek veri: uçtan uca çözülebilirlik ----------

    @Test
    fun random500PlusFirstAndLastSolveEndToEnd() {
        val rnd = Random(20261025)
        val idx = (listOf(0, 1, 9998, 9999) + List(500) { rnd.nextInt(real.count) }).distinct()
        for (i in idx) {
            val level = real.get(i)!!
            val e = GameEngine(level, realDict, random = Random(i))
            assertEquals("seviye ${level.id}", level.words.map { it.word }.toSet().size, e.remainingWords)
            assertTrue(!e.isComplete())
            for (w in level.words.shuffled(Random(i))) {
                val r = e.submit(TurkishText.upper(w.word))
                // aynı kelime iki kez yerleştirilmişse ikincisi AlreadyFound olabilir
                assertTrue("seviye ${level.id} ${w.word}: $r", r is SubmitResult.Correct || r is SubmitResult.AlreadyFound)
            }
            assertTrue("seviye ${level.id} tamamlanmadı", e.isComplete())
            assertEquals(0, e.remainingWords)
            assertEquals(0, e.bonusCount)
            for (w in level.words) for ((r, c) in w.cells())
                assertEquals(CellState.Filled, e.cellState(r, c))
            // kapanıp açılınca da tamam
            val s = e.snapshot()
            assertTrue(GameEngine(level, realDict, s.found, s.bonusFound, s.revealed).isComplete())
        }
    }

    @Test
    fun hintsAloneCanAlsoCompleteRealLevels() {
        for (i in listOf(0, 4999, 9999)) {
            val level = real.get(i)!!
            val e = GameEngine(level, realDict, random = Random(3))
            var guard = 0
            while (e.revealWord() != null) assertTrue(++guard < 100)
            assertTrue(e.isComplete())
            assertNull(e.revealLetter())
        }
    }

    @Test
    fun realLevelsBonusWordsWork() {
        val rnd = Random(7)
        var bonusSeen = 0
        repeat(100) {
            val level = real.get(rnd.nextInt(real.count))!!
            val grid = level.words.map { it.word }.toSet()
            val extra = realDict.spellable(level.letters).firstOrNull { it !in grid } ?: return@repeat
            val e = GameEngine(level, realDict)
            assertEquals(SubmitResult.Bonus(extra), e.submit(extra))
            assertEquals(SubmitResult.AlreadyFound(extra), e.submit(extra))
            assertEquals(1, e.bonusCount)
            assertEquals(grid.size, e.remainingWords)
            bonusSeen++
        }
        assertTrue(bonusSeen > 50)
    }

    // ---------- gerçek veri: yapısal sözleşme (10.000 seviyenin hepsi) ----------

    @Test
    fun allLevelsStructural() {
        assertEquals(10000, allLevels.size)
        val bad = ArrayList<String>()
        allLevels.forEachIndexed { i, l ->
            if (l.id != i + 1) bad += "id ${l.id} != ${i + 1}"
            if (l.letters.size !in 3..7) bad += "${l.id}: harf sayısı ${l.letters.size}"
            if (l.words.size < 2) bad += "${l.id}: ${l.words.size} kelime"
            if (l.words.size > 16) bad += "${l.id}: ${l.words.size} kelime > 16"
            if (l.words.map { it.word }.toSet().size != l.words.size) bad += "${l.id}: tekrar eden kelime"
            if (l.letters.any { it !in TurkishText.ALPHABET }) bad += "${l.id}: alfabe dışı harf"
            for (w in l.words) {
                if (w.word.length < 2) bad += "${l.id}: ${w.word} kısa"
                if (!TurkishText.canSpell(w.word, l.letters)) bad += "${l.id}: ${w.word} havuzdan yazılamaz"
                if (w.row < 0 || w.col < 0) bad += "${l.id}: negatif konum ${w.word}"
                if (w.word.any { it !in TurkishText.ALPHABET }) bad += "${l.id}: ${w.word} alfabe dışı"
            }
        }
        assertTrue("${bad.size} sorun, ilk 10: ${bad.take(10)}", bad.isEmpty())
    }

    @Test
    fun allGridWordsAreInRealDictionary() {
        val bad = allLevels.flatMap { l -> l.words.filter { !realDict.contains(it.word) }.map { "${l.id}:${it.word}" } }
        assertTrue("${bad.size} kelime sözlükte yok, ilk 10: ${bad.take(10)}", bad.isEmpty())
    }

    @Test
    fun noUnplannedAdjacentRunsInAnyLevel() {
        val bad = ArrayList<String>()
        for (l in allLevels) {
            val grid = HashMap<Pair<Int, Int>, Char>()
            for (w in l.words) w.cells().forEachIndexed { i, c -> grid[c] = w.word[i] }
            val planned = l.words.map { Triple(it.word, it.row to it.col, it.horizontal) }.toSet()
            val maxR = grid.keys.maxOf { it.first }
            val maxC = grid.keys.maxOf { it.second }
            for (horizontal in listOf(true, false)) {
                val outer = if (horizontal) maxR else maxC
                val inner = if (horizontal) maxC else maxR
                for (a in 0..outer) {
                    var b = 0
                    while (b <= inner) {
                        fun cell(x: Int) = if (horizontal) (a to x) else (x to a)
                        if (cell(b) !in grid) { b++; continue }
                        val start = b
                        val sb = StringBuilder()
                        while (b <= inner && cell(b) in grid) { sb.append(grid[cell(b)]); b++ }
                        if (sb.length >= 2 && Triple(sb.toString(), cell(start), horizontal) !in planned)
                            bad += "${l.id}: plansız ${if (horizontal) "yatay" else "dikey"} dizi '$sb' @${cell(start)}"
                    }
                }
            }
        }
        assertTrue("${bad.size} sorun, ilk 10: ${bad.take(10)}", bad.isEmpty())
    }

    @Test
    fun allLevelsAreConnectedThroughCrossings() {
        val bad = ArrayList<Int>()
        for (l in allLevels) {
            val cellSets = l.words.map { it.cells().toSet() }
            val seen = BooleanArray(cellSets.size)
            val stack = ArrayDeque<Int>().apply { add(0); seen[0] = true }
            while (stack.isNotEmpty()) {
                val x = stack.removeLast()
                for (y in cellSets.indices) if (!seen[y] && cellSets[x].any { it in cellSets[y] }) { seen[y] = true; stack.add(y) }
            }
            if (seen.any { !it }) bad += l.id
        }
        assertTrue("bağlantısız ${bad.size} seviye: ${bad.take(10)}", bad.isEmpty())
    }

    @Test
    fun allLevelsAreUnique() {
        val keys = HashMap<String, Int>()
        val dup = ArrayList<String>()
        for (l in allLevels) {
            val k = l.letters.sorted().joinToString("") + "|" + l.words.map { it.word }.sorted().joinToString(",")
            keys.put(k, l.id)?.let { dup += "${l.id}==$it" }
        }
        assertTrue("tekrar ${dup.size}: ${dup.take(10)}", dup.isEmpty())
    }

    @Test
    fun difficultyCurveStartIsEasyAndEndIsHard() {
        for (l in allLevels.take(50)) {
            assertTrue("${l.id} harf ${l.letters.size}", l.letters.size in 3..4)
            assertTrue("${l.id} kelime ${l.words.size}", l.words.size in 2..4)
        }
        val tail = allLevels.takeLast(500)
        assertTrue("son 500'de 7 harfli yok", tail.any { it.letters.size == 7 })
        assertTrue(tail.map { it.words.size }.average() > allLevels.take(500).map { it.words.size }.average())
    }

    @Test
    fun assetSizeUnder6MB() {
        assertTrue(TestAssets.file("levels.txt.gz").length() < 6L * 1024 * 1024)
    }

    // ---------- bozuk / boş / kötü niyetli akışlar ----------

    @Test
    fun openCalledOnceEvenWithManyAccesses() {
        var calls = 0
        val bytes = gz("1|nte|ten,0,0,y;net,0,2,d\n")
        val r = LevelRepository { calls++; ByteArrayInputStream(bytes) }
        repeat(5) { r.count; r.get(0); r.get(5) }
        assertEquals(1, calls)
    }

    @Test
    fun streamIsClosedAfterSuccessfulLoad() {
        var closed = false
        val bytes = gz("1|nte|ten,0,0,y;net,0,2,d\n")
        val r = LevelRepository { object : FilterInputStream(ByteArrayInputStream(bytes)) { override fun close() { closed = true; super.close() } } }
        assertEquals(1, r.count)
        assertTrue("akış kapatılmadı", closed)
    }

    @Test
    fun streamIsClosedWhenContentIsNotGzip() {
        var closed = false
        val r = LevelRepository { object : FilterInputStream(ByteArrayInputStream("düz metin, gzip değil".toByteArray())) { override fun close() { closed = true; super.close() } } }
        assertEquals(0, r.count)
        assertTrue("gzip olmayan akış kapatılmadı (kaynak sızıntısı)", closed)
    }

    @Test
    fun streamIsClosedWhenGzipIsCorruptedMidway() {
        var closed = false
        val full = gz((1..3000).joinToString("\n") { "$it|nte|ten,0,0,y;net,0,2,d" })
        val bad = full.copyOf().also { for (i in full.size / 2 until full.size / 2 + 50) it[i] = (it[i] + 1).toByte() }
        val r = LevelRepository { object : FilterInputStream(ByteArrayInputStream(bad)) { override fun close() { closed = true; super.close() } } }
        assertTrue(r.count in 0..3000)
        assertTrue(closed)
    }

    @Test
    fun corruptedDeflateDataDoesNotCrash() {
        val full = gz((1..3000).joinToString("\n") { "$it|nte|ten,0,0,y;net,0,2,d" })
        val rnd = Random(5)
        repeat(30) {
            val b = full.copyOf()
            repeat(10) { b[10 + rnd.nextInt(b.size - 10)] = rnd.nextInt().toByte() }
            val r = LevelRepository { ByteArrayInputStream(b) }
            assertTrue(r.count in 0..3000)
            assertNull(r.get(3000))
            for (i in 0 until r.count) assertNotNull(r.get(i))
        }
    }

    @Test
    fun truncatedGzipKeepsOnlyCompleteValidLevels() {
        val lines = (1..4000).map { "$it|nte|ten,0,0,y;net,0,2,d" }
        val full = gz(lines.joinToString("\n"))
        for (cut in listOf(10, 30, full.size / 3, full.size / 2, full.size - 20, full.size - 1)) {
            val r = LevelRepository { ByteArrayInputStream(full.copyOf(cut)) }
            for (i in 0 until r.count) {
                val l = r.get(i)!!
                assertEquals(i + 1, l.id)
                assertEquals(2, l.words.size) // yarım satır geçerli seviye gibi görünmemeli
            }
        }
    }

    @Test
    fun invalidUtf8LineSkippedOthersKept() {
        val bytes = "1|nte|ten,0,0,y;net,0,2,d\n".toByteArray() +
            byteArrayOf(0x32, 0x7C, 0xFF.toByte(), 0xFE.toByte(), 0x7C, 0xC3.toByte(), 0x0A) +
            "3|etm|met,2,0,y;tem,0,0,d\n".toByteArray()
        val r = LevelRepository { ByteArrayInputStream(gzBytes(bytes)) }
        assertEquals(listOf(1, 3), (0 until r.count).map { r.get(it)!!.id })
    }

    @Test
    fun bomAndWindowsAndWhitespaceNoise() {
        val r = repo("﻿1|nte|ten,0,0,y;net,0,2,d\r\n  2|etm|met,2,0,y;tem,0,0,d  \r\n\r\n   \n")
        assertEquals("BOM'lu ilk satır kaybolmamalı", listOf(1, 2), (0 until r.count).map { r.get(it)!!.id })
    }

    @Test
    fun uppercaseInputAndDottedI() {
        val r = repo("1|NTE|TEN,0,0,y;NET,0,2,d\n2|İSİ|isi,0,0,y;sii,0,1,d\n3|ISI|ısı,0,0,y;sı,0,1,d\n")
        assertEquals(listOf('n', 't', 'e'), r.get(0)!!.letters)
        assertEquals("ten", r.get(0)!!.words[0].word)
        assertEquals(3, r.count) // "isi" havuz İSİ -> i,s,i ; "sii" havuzdan yazılamaz değil: s,i,i var
        assertEquals(listOf('i', 's', 'i'), r.get(1)!!.letters)
        assertEquals(listOf('ı', 's', 'ı'), r.get(2)!!.letters)
    }

    @Test
    fun dottedIPoolDoesNotAcceptDotlessWords() {
        val r = repo("1|ISI|isi,0,0,y;sıs,0,1,d\n")
        assertEquals(0, r.count)
    }

    @Test
    fun hugeNumbersDoNotCrashOrOverflowIntoAcceptedGarbage() {
        val r = repo("1|nte|ten,99999999999,0,y;net,0,2,d\n2|nte|ten,2147483647,2147483647,y;net,0,2,d\n3|nte|ten,0,0,y;net,0,2,d\n")
        // 1 geçersiz (taşma). 2 numaralı: koordinat taşması negatif kutu üretmemeli.
        val ids = (0 until r.count).map { r.get(it)!!.id }
        assertTrue(1 !in ids)
        assertTrue("taşan koordinatlı seviye kabul edildi: $ids", 2 !in ids)
        assertTrue(3 in ids)
    }

    @Test
    fun extraFieldsAndEmptySegmentsAreRejectedNotCrashed() {
        val text = listOf(
            "1|nte|ten,0,0,y;net,0,2,d|extra",
            "2|nte|ten,0,0,y;;net,0,2,d",
            "3|nte|ten,0,0,y;net,0,2,d;",
            "4|nte|ten,0,0,y,z",
            "5|nte|,0,0,y",
            "||",
            "|",
            "\u0000\u0000",
            "6|nte|ten,0,0,y;net,0,2,d",
        ).joinToString("\n")
        val r = repo(text)
        assertEquals(listOf(6), (0 until r.count).map { r.get(it)!!.id })
    }

    @Test
    fun veryLongLevelListLoads() {
        val r = repo((1..50000).joinToString("\n") { "$it|nte|ten,0,0,y;net,0,2,d" })
        assertEquals(50000, r.count)
        assertEquals(50000, r.get(49999)!!.id)
        assertNull(r.get(50000))
        assertNull(r.get(Int.MIN_VALUE))
        assertNull(r.get(Int.MAX_VALUE))
    }

    @Test
    fun ioExceptionFromReadDoesNotCrash() {
        val r = LevelRepository { object : InputStream() { override fun read(): Int = throw IOException("disk") } }
        assertEquals(0, r.count)
    }
}
