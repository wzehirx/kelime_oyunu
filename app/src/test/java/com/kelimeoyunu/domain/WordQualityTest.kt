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

    /** Bağımsız QA raporu (2026-10): kabul setinden TAMAMEN çıkarılan sözcükler (çekimleriyle). */
    @Test
    fun qaProfanityAndInsultsAreFullyRemoved() {
        val removed = ("düzüş düzüşen düzüşme düzüşüp işemek işeme işedi işer işeyen frikik ibiş kaşar kaşara kavata kerata " +
            "deyyus dallama kıç kıça kıçı kıçım kıçında kodumu kodumun kodumuz anan ananı ananın ananla ananızı anasını " +
            "anasına bacın avrat hödük kıro osuruk gebert zıkkım yuh oha lan ulan penis penisi penise vajina anüs sperm " +
            "sperma porno orgazm kondom mastur masturi seksisi seksüel hitler hitleri çingene kafir kafirce").split(" ")
        val accepted = removed.filter { dict.contains(it) }
        assertTrue("kabul edilen QA sözcükleri: $accepted", accepted.isEmpty())
        // bağlam sözcükleri engellenmedi
        for (w in listOf("iş", "işe", "işte", "bacı", "ananas", "anason", "kod", "kıçüstü")) {
            assertTrue("$w kabul edilmeli", dict.contains(w))
        }
    }

    @Test
    fun qaMildInsultsStayAcceptedButNeverInGrid() {
        val gridWords = (0 until levels.count).flatMap { levels.get(it)!!.words.map { w -> w.word } }.toHashSet()
        for (w in "hıyar öküz moruk gerzek embesil zampara lavuk geber defol sümük".split(" ")) {
            assertTrue("$w kabul edilmeli", dict.contains(w))
            assertFalse("$w havuzda", w in pool)
            assertFalse("$w ızgarada", w in gridWords)
        }
        assertTrue("ud kabul edilmeli", dict.contains("ud"))
    }

    @Test
    fun qaGridExclusionsNeverAppearInAnyLevel() {
        val excluded = ("penis porno orgazm sperm kondom taciz cinsel zina meni testis kıç meme döl kasık sütyen bakire " +
            "metres çiş apış osur kaka kak kuka halt lavuk zampara yalaka yalak geberik geber moruk hıyar gerzek embesil " +
            "defol çirkef leş ali eren ayla kaya mine emin rina rami nisa sena alperen asuman hande nafiz feyzal gökmen " +
            "bahadır aksu aut bop bre flu gri ski uzo yoo jig jip jem jen jul jüt rua ruz rop pog poy zom şom şua evç ekü " +
            "ayni kani kari akil akim ari ati atik adi bati bani").split(" ").toHashSet()
        assertTrue("havuzda kalan: ${excluded.filter { it in pool }}", excluded.none { it in pool })
        for (i in 0 until levels.count) {
            val l = levels.get(i)!!
            val bad = l.words.map { it.word }.filter { it in excluded }
            assertTrue("seviye ${l.id}: $bad", bad.isEmpty())
        }
    }

    /** Erken seviyeler yaygın sözcüklerden kurulur: id <= 60 -> havuz sırası < 2000, id <= 700 -> < 3000. */
    @Test
    fun earlyLevelsUseCommonWords() {
        val rank = TestAssets.repoWordList("tools/words/grid_pool.txt").withIndex().associate { it.value to it.index }
        var worst60 = -1
        var worst700 = -1
        for (i in 0 until 700) {
            val l = levels.get(i)!!
            for (w in l.words) {
                val r = rank[w.word] ?: error("seviye ${l.id}: ${w.word} havuzda yok")
                if (l.id <= 60) worst60 = maxOf(worst60, r)
                worst700 = maxOf(worst700, r)
            }
        }
        println("EarlyRank: id<=60 en büyük sıra $worst60, id<=700 en büyük sıra $worst700")
        assertTrue("id<=60 sıra $worst60", worst60 < 2000)
        assertTrue("id<=700 sıra $worst700", worst700 < 3000)
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
