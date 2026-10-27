package com.kelimeoyunu.domain

import com.kelimeoyunu.TestAssets
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.loadWords
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.4 kullanıcı hatası regresyonları, GERÇEK words.txt / levels.txt.gz / grid_pool.txt / blocklist.txt ile:
 * "sat" kabul edilmiyordu, "am" gibi argo kabul ediliyordu, "ast" gibi nadir sözcük ızgaraya giriyordu.
 */
class WordQualityTest {
    private val dict by lazy { loadWords { TestAssets.open("words.txt") } }
    private val levels by lazy { LevelRepository { TestAssets.open("levels.txt.gz") } }
    private val blocklist by lazy { TestAssets.repoWordList("tools/words/blocklist.txt") }
    private val pool by lazy { TestAssets.repoWordList("tools/words/grid_pool.txt").toHashSet() }

    @Test
    fun verbRootsAndInflectedFormsAreAccepted() {
        for (w in listOf("sat", "yat", "tas", "taş", "evler", "kitap", "gel", "yaz", "evlerde", "kitabı", "SAT", "Taş")) {
            assertTrue("$w kabul edilmeli", dict.contains(w))
        }
    }

    @Test
    fun slangIsRejected() {
        for (w in listOf("am", "sik", "göt", "bok", "piç", "çük", "ibne", "puşt", "yarak", "AM", "Sik")) {
            assertFalse("$w reddedilmeli", dict.contains(w))
        }
    }

    @Test
    fun entireBlocklistIsRejected() {
        assertTrue("blocklist okunamadı: ${blocklist.size}", blocklist.size >= 90)
        val accepted = blocklist.filter { dict.contains(it) }
        assertTrue("kabul edilen engelli kelimeler: $accepted", accepted.isEmpty())
    }

    @Test
    fun dictionaryHasExpectedSizeAndLoadsFast() {
        val t0 = System.nanoTime()
        val d = loadWords { TestAssets.open("words.txt") }
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("DictionaryLoad: ${d.size} kelime, $ms ms")
        assertTrue("kabul seti ${d.size}", d.size in 50_000..120_000)
        assertTrue("yükleme $ms ms", ms < 1_000)
    }

    @Test
    fun rareWordAstIsAcceptedButNeverInTheGrid() {
        assertTrue(dict.contains("ast"))
        assertFalse("ast ızgara havuzunda", "ast" in pool)
        for (i in 0 until levels.count) {
            val l = levels.get(i)!!
            assertFalse("seviye ${l.id}: ast ızgarada", l.words.any { it.word == "ast" })
        }
    }

    @Test
    fun everyGridWordIsInPoolAndDictionaryAndNotBlocked() {
        val blocked = blocklist.toHashSet()
        for (i in 0 until levels.count) {
            val l = levels.get(i)!!
            for (w in l.words) {
                assertTrue("seviye ${l.id}: ${w.word} havuzda yok", w.word in pool)
                assertTrue("seviye ${l.id}: ${w.word} kabul setinde yok", dict.contains(w.word))
                assertFalse("seviye ${l.id}: ${w.word} engelli", w.word in blocked)
            }
        }
    }

    /** Kullanıcı: 37. seviyede (t, a, s) "sat" yazınca kabul edilmedi. Harfleri yazılabilen her sözlük kelimesi motorda geçerli olmalı. */
    @Test
    fun tasLevelAcceptsSatYatTasTas() {
        val letters = listOf('t', 'a', 's')
        val level = Level(37, letters, listOf(PlacedWord("tas", 0, 0, true), PlacedWord("sat", 0, 0, false)))
        val engine = GameEngine(level, dict)
        for (w in dict.spellable(letters, min = 2)) {
            val r = GameEngine(level, dict).submit(w)
            assertTrue("$w -> $r", r is SubmitResult.Correct || r is SubmitResult.Bonus)
        }
        assertEquals(SubmitResult.Correct("sat"), engine.submit("sat"))
        assertEquals(SubmitResult.Correct("tas"), engine.submit("tas"))
        assertTrue(engine.isComplete())
        assertTrue(dict.spellable(letters, min = 2).containsAll(listOf("at", "as", "sat", "tas", "ast")))
    }

    @Test
    fun noSpellableDictionaryWordIsInvalidOnRandomRealLevels() {
        val rnd = Random(2024)
        val picks = List(300) { rnd.nextInt(levels.count) }.toSet()
        var checked = 0
        for (i in picks) {
            val level = levels.get(i)!!
            val spellable = dict.spellable(level.letters, min = 2)
            val gridWords = level.words.map { it.word }.toSet()
            assertTrue("seviye ${level.id}: ızgara kelimeleri yazılabilir değil", spellable.containsAll(gridWords))
            val engine = GameEngine(level, dict)
            for (w in spellable) {
                val r = engine.submit(w)
                assertFalse("seviye ${level.id}: $w -> $r", r is SubmitResult.Invalid || r === SubmitResult.Empty)
                if (w in gridWords) assertTrue("$w", r is SubmitResult.Correct) else assertTrue("$w -> $r", r is SubmitResult.Bonus)
                checked++
            }
            assertTrue(engine.isComplete())
        }
        println("WordQuality: ${picks.size} seviye, $checked kelime denendi")
    }
}
