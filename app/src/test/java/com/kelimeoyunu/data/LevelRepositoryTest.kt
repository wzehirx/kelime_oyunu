package com.kelimeoyunu.data

import com.kelimeoyunu.TestAssets
import com.kelimeoyunu.domain.CellState
import com.kelimeoyunu.domain.GameEngine
import com.kelimeoyunu.domain.PlacedWord
import com.kelimeoyunu.domain.TurkishText
import com.kelimeoyunu.domain.WordDictionary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRepositoryTest {
    private fun gz(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    private fun repo(text: String) = LevelRepository { ByteArrayInputStream(gz(text)) }

    @Test
    fun parsesLevels() {
        val r = repo("1|uhş|şuh,1,0,y;huş,0,1,d\n2|nte|ten,0,0,y;net,0,2,d\n")
        assertEquals(2, r.count)
        val l = r.get(0)!!
        assertEquals(1, l.id)
        assertEquals(listOf('u', 'h', 'ş'), l.letters)
        assertEquals(
            listOf(PlacedWord("şuh", 1, 0, true), PlacedWord("huş", 0, 1, false)),
            l.words,
        )
        assertEquals(2, r.get(1)!!.id)
    }

    @Test
    fun outOfRangeIsNull() {
        val r = repo("1|nte|ten,0,0,y;net,0,2,d\n")
        assertNull(r.get(1))
        assertNull(r.get(-1))
        assertNull(r.get(10000))
    }

    @Test
    fun unicodeAndCrLfAndTrailingBlankLines() {
        val r = repo("1|ıkş|şık,0,0,y;kış,0,2,d\r\n\r\n\n")
        assertEquals(1, r.count)
        assertEquals('ı', r.get(0)!!.letters[0])
    }

    @Test
    fun corruptLinesAreSkippedAndNotCounted() {
        val text = listOf(
            "1|nte|ten,0,0,y;net,0,2,d",
            "garbage",
            "3|nte|ten,x,0,y",              // sayı değil
            "4|nte|ten,0,0,q",              // bilinmeyen yön
            "5|nte|",                       // kelime yok
            "6||ten,0,0,y",                 // harf yok
            "7|nte|abc,0,0,y",              // havuzdan yazılamaz
            "8|nte|ten,0,0,y;net,0,0,d",    // kesişimde harf çakışması (t != n)
            "abc|nte|ten,0,0,y",            // id sayı değil
            "9|nte|ten,-1,0,y",             // negatif konum
            "2|etm|met,2,0,y;tem,0,0,d",
        ).joinToString("\n")
        val r = repo(text)
        assertEquals(2, r.count)
        assertEquals(1, r.get(0)!!.id)
        assertEquals(2, r.get(1)!!.id)
        assertNull(r.get(2))
    }

    @Test
    fun emptyStreamsDoNotCrash() {
        val empty = LevelRepository { ByteArrayInputStream(ByteArray(0)) }
        assertEquals(0, empty.count)
        assertNull(empty.get(0))

        val emptyGzip = repo("")
        assertEquals(0, emptyGzip.count)
        assertNull(emptyGzip.get(0))
    }

    @Test
    fun notGzipOrMissingAssetDoesNotCrash() {
        val notGzip = LevelRepository { ByteArrayInputStream("1|nte|ten,0,0,y".toByteArray()) }
        assertEquals(0, notGzip.count)
        assertNull(notGzip.get(0))

        val missing = LevelRepository { throw IOException("yok") }
        assertEquals(0, missing.count)
        assertNull(missing.get(0))
    }

    @Test
    fun truncatedGzipKeepsLevelsReadSoFarOrNone() {
        val full = gz((1..2000).joinToString("\n") { "$it|nte|ten,0,0,y;net,0,2,d" })
        val cut = full.copyOf(full.size / 2)
        val r = LevelRepository { ByteArrayInputStream(cut) }
        assertTrue(r.count in 0..2000)
        assertNull(r.get(2000))
    }

    // --- gerçek asset ---

    private val real by lazy { LevelRepository { TestAssets.open("levels.txt.gz") } }

    @Test
    fun realFileHas10000Levels() {
        assertEquals(10000, real.count)
        assertEquals(10000, real.get(9999)?.id)
        assertEquals(1, real.get(0)?.id)
        assertNull(real.get(10000))
    }

    @Test
    fun realFirstAndLastLevelsAreSelfConsistent() {
        for (i in listOf(0, 9999)) {
            val level = real.get(i)!!
            assertTrue(level.letters.size in 3..7)
            assertTrue(level.words.size >= 2)
            level.words.forEach {
                assertTrue("${it.word} havuzdan yazılamıyor", TurkishText.canSpell(it.word, level.letters))
            }
            val dict = WordDictionary(level.words.map { it.word }.toSet())
            val e = GameEngine(level, dict)
            level.words.forEach { assertNotNull(e.submit(it.word)) }
            assertTrue(e.isComplete())
            assertEquals(CellState.Filled, e.cellState(level.words[0].row, level.words[0].col))
        }
    }

    @Test
    fun realLevelsIdsAreSequential() {
        for (i in 0 until real.count step 97) assertEquals(i + 1, real.get(i)!!.id)
    }

}
