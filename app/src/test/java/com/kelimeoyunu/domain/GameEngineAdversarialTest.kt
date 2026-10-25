package com.kelimeoyunu.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineAdversarialTest {
    // kalem yatay (0,0..4); lam dikey (0..2, 2); kesişim (0,2)='l'
    private val level = Level(
        7, listOf('k', 'a', 'l', 'e', 'm'),
        listOf(PlacedWord("kalem", 0, 0, true), PlacedWord("lam", 0, 2, false)),
    )
    private val dict = WordDictionary(setOf("kalem", "lam", "elma", "mal", "ela", "ala", "kal", "ısı"))
    private fun engine(seed: Int = 1) = GameEngine(level, dict, random = Random(seed))
    private val allCells = (-1..5).flatMap { r -> (-1..6).map { c -> r to c } }

    @Test
    fun sameGridWordTwiceAndBonusTwice() {
        val e = engine()
        assertEquals(SubmitResult.Correct("kalem"), e.submit("kalem"))
        assertEquals(SubmitResult.AlreadyFound("kalem"), e.submit("KALEM"))
        assertEquals(SubmitResult.AlreadyFound("kalem"), e.submit(" kalem "))
        assertEquals(1, e.remainingWords)
        assertEquals(SubmitResult.Bonus("mal"), e.submit("MAL"))
        assertEquals(SubmitResult.AlreadyFound("mal"), e.submit("mal"))
        assertEquals(1, e.bonusCount)
    }

    @Test
    fun bonusCountedExactlyOnceAcrossManyDistinctBonuses() {
        val e = engine()
        for (w in listOf("elma", "mal", "ela", "kal")) assertEquals(SubmitResult.Bonus(w), e.submit(w))
        assertEquals(4, e.bonusCount)
        repeat(3) { for (w in listOf("elma", "mal", "ela", "kal")) e.submit(w) }
        assertEquals(4, e.bonusCount)
        assertEquals(2, e.remainingWords)
        assertFalse(e.isComplete())
    }

    @Test
    fun invalidDoesNotChangeState() {
        val e = engine()
        val before = e.snapshot()
        assertEquals(SubmitResult.Invalid("zzz"), e.submit("zzz"))
        assertEquals(SubmitResult.Invalid("kalemm"), e.submit("kalemm")) // fazla harf
        assertEquals(SubmitResult.Invalid("kakem"), e.submit("kakem"))
        assertEquals(SubmitResult.Invalid("ısı"), e.submit("ısı"))      // sözlükte var, havuzda yok
        assertEquals(before, e.snapshot())
        assertEquals(0, e.bonusCount)
    }

    @Test
    fun emptyVariants() {
        val e = engine()
        for (s in listOf("", " ", "\t", "\n", " \r\n ", " ")) assertEquals("'$s'", SubmitResult.Empty, e.submit(s))
    }

    @Test
    fun crossingCellsFilledByEitherWordAndOthersStayHidden() {
        val e = engine()
        assertEquals(CellState.Hidden, e.cellState(0, 2))
        e.submit("lam")
        assertEquals(CellState.Filled, e.cellState(0, 2))
        assertEquals(CellState.Filled, e.cellState(1, 2))
        assertEquals(CellState.Filled, e.cellState(2, 2))
        // kalem'in kesişmeyen kutuları hala kapalı
        for (c in listOf(0, 1, 3, 4)) assertEquals(CellState.Hidden, e.cellState(0, c))
        e.submit("kalem")
        for (c in 0..4) assertEquals(CellState.Filled, e.cellState(0, c))
        assertTrue(e.isComplete())
    }

    @Test
    fun cellsOutsideGridAreEmpty() {
        val e = engine()
        for ((r, c) in allCells) {
            val inGrid = level.words.any { (r to c) in it.cells() }
            if (!inGrid) assertEquals("($r,$c)", CellState.Empty, e.cellState(r, c))
        }
        assertEquals(CellState.Empty, e.cellState(Int.MIN_VALUE, Int.MAX_VALUE))
    }

    @Test
    fun revealLetterRevealsEachHiddenCellExactlyOnceThenNull() {
        val e = engine()
        // ızgarada 5 + 3 - 1 = 7 benzersiz kutu
        val got = mutableListOf<Pair<Int, Int>>()
        repeat(7) {
            val c = e.revealLetter()
            assertNotNull("tur $it", c)
            assertEquals(CellState.Revealed, e.cellState(c!!.first, c.second))
            got += c
        }
        assertEquals(7, got.toSet().size)
        assertNull(e.revealLetter())
        assertNull(e.revealLetter())
        assertEquals(7, e.snapshot().revealed.size)
    }

    @Test
    fun revealLetterSkipsFilledCells() {
        val e = engine()
        e.submit("kalem")
        val got = (1..10).mapNotNull { e.revealLetter() }
        assertEquals(listOf(1 to 2, 2 to 2).toSet(), got.toSet())
        assertEquals(2, got.size)
    }

    @Test
    fun revealLetterWhenAllFoundIsNull() {
        val e = engine()
        e.submit("kalem"); e.submit("lam")
        assertNull(e.revealLetter())
        assertTrue(e.snapshot().revealed.isEmpty())
    }

    @Test
    fun revealLetterThenFoundBecomesFilled() {
        val e = engine()
        val c = e.revealLetter()!!
        val owner = level.words.first { c in it.cells() }
        e.submit(owner.word)
        assertEquals(CellState.Filled, e.cellState(c.first, c.second))
    }

    @Test
    fun revealWordWithAllLettersRevealedStillWorks() {
        val e = engine()
        repeat(7) { e.revealLetter() }
        assertNull(e.revealLetter())
        val w1 = e.revealWord()
        assertNotNull(w1)
        val w2 = e.revealWord()
        assertNotNull(w2)
        assertTrue(w1 != w2)
        assertNull(e.revealWord())
        assertTrue(e.isComplete())
        assertEquals(0, e.remainingWords)
    }

    @Test
    fun revealWordAfterAllWordsFoundIsNullAndSubmitStillBehaves() {
        val e = engine()
        e.submit("kalem"); e.submit("lam")
        assertTrue(e.isComplete())
        assertNull(e.revealWord())
        assertEquals(SubmitResult.AlreadyFound("kalem"), e.submit("kalem"))
        assertEquals(SubmitResult.AlreadyFound("lam"), e.submit("lam"))
        assertEquals(SubmitResult.Bonus("elma"), e.submit("elma"))     // seviye bitse de bonus sayılır
        assertEquals(SubmitResult.Invalid("zzz"), e.submit("zzz"))
        assertEquals(SubmitResult.Empty, e.submit(""))
        assertTrue(e.isComplete())
        assertEquals(0, e.remainingWords)
    }

    @Test
    fun revealWordNeverRepeatsAndMarksExactlyThatWord() {
        val e = engine()
        val w = e.revealWord()!!
        assertEquals(setOf(w.word), e.snapshot().found)
        assertEquals(SubmitResult.AlreadyFound(w.word), e.submit(w.word))
        for ((r, c) in w.cells()) assertEquals(CellState.Filled, e.cellState(r, c))
    }

    @Test
    fun snapshotRebuildIsEquivalentAtEveryStep() {
        val ops = listOf<(GameEngine) -> Unit>(
            { it.submit("elma") }, { it.revealLetter() }, { it.submit("lam") }, { it.revealLetter() },
            { it.submit("zzz") }, { it.submit("mal") }, { it.revealLetter() }, { it.submit("kalem") },
            { it.revealLetter() }, { it.revealWord() },
        )
        val e = engine()
        for ((i, op) in ops.withIndex()) {
            op(e)
            val s = e.snapshot()
            val r = GameEngine(level, dict, s.found, s.bonusFound, s.revealed)
            assertEquals("adım $i", s, r.snapshot())
            assertEquals(e.remainingWords, r.remainingWords)
            assertEquals(e.bonusCount, r.bonusCount)
            assertEquals(e.isComplete(), r.isComplete())
            for ((row, col) in allCells) assertEquals("adım $i ($row,$col)", e.cellState(row, col), r.cellState(row, col))
        }
    }

    @Test
    fun rebuiltEngineBehavesIdenticallyOnSameInputs() {
        val e = engine()
        e.submit("kalem"); e.submit("elma"); e.revealLetter()
        val s = e.snapshot()
        val r = GameEngine(level, dict, s.found, s.bonusFound, s.revealed)
        for (w in listOf("kalem", "elma", "lam", "mal", "x", "")) assertEquals(w, e.submit(w), r.submit(w))
        assertEquals(e.snapshot(), r.snapshot())
    }

    @Test
    fun constructorCopiesInputSets() {
        val found = mutableSetOf<String>()
        val e = GameEngine(level, dict, found)
        e.submit("kalem")
        assertTrue(found.isEmpty())
        found += "lam"
        assertEquals(1, e.remainingWords)
    }

    @Test
    fun restoredStateWithUnknownEntriesIsHarmless() {
        val e = GameEngine(level, dict, setOf("yok"), setOf("başka"), setOf(99 to 99, 0 to 0))
        assertEquals(2, e.remainingWords)
        assertEquals(1, e.bonusCount)
        assertEquals(CellState.Empty, e.cellState(99, 99))
        assertEquals(CellState.Revealed, e.cellState(0, 0))
        // 6 kapalı kutu: (0,0) zaten açık, 99,99 ızgarada değil
        assertEquals(6, generateSequence { e.revealLetter() }.take(20).count())
    }

    @Test
    fun duplicatePlacedWordCountsOnceButFillsBoth() {
        val l = Level(1, listOf('a', 'l', 'a'), listOf(PlacedWord("ala", 0, 0, true), PlacedWord("ala", 2, 0, true)))
        val e = GameEngine(l, WordDictionary(setOf("ala")))
        assertEquals(1, e.remainingWords)
        assertEquals(SubmitResult.Correct("ala"), e.submit("ala"))
        assertTrue(e.isComplete())
        for (c in 0..2) { assertEquals(CellState.Filled, e.cellState(0, c)); assertEquals(CellState.Filled, e.cellState(2, c)) }
    }

    @Test
    fun gridWordNotInDictionaryIsStillCorrect() {
        val e = GameEngine(level, WordDictionary(emptySet()))
        assertEquals(SubmitResult.Correct("kalem"), e.submit("kalem"))
    }

    @Test
    fun repeatedLetterPoolAndIsiInEngine() {
        val l = Level(3, listOf('a', 'l'), listOf(PlacedWord("al", 0, 0, true), PlacedWord("la", 0, 1, false)))
        val e = GameEngine(l, WordDictionary(setOf("al", "la", "ala")))
        assertEquals(SubmitResult.Invalid("ala"), e.submit("ala"))
        assertEquals(SubmitResult.Invalid("ala"), e.submit("ALA"))
        val l2 = Level(4, listOf('I', 'S', 'I'), listOf(PlacedWord("ısı", 0, 0, true), PlacedWord("sı", 0, 1, false)))
        val e2 = GameEngine(l2, WordDictionary(setOf("ısı", "isi", "sı")))
        assertEquals(SubmitResult.Invalid("isi"), e2.submit("isi"))
        assertEquals(SubmitResult.Invalid("isi"), e2.submit("İSİ"))
        assertEquals(SubmitResult.Correct("ısı"), e2.submit("ISI"))
    }
}
