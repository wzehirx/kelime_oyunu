package com.kelimeoyunu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    // kalem yatay (0,0)-(0,4); lam dikey (0,2)-(2,2), (0,2) ortak 'l'
    private val level = Level(
        id = 7,
        letters = listOf('k', 'a', 'l', 'e', 'm'),
        words = listOf(
            PlacedWord("kalem", 0, 0, true),
            PlacedWord("lam", 0, 2, false),
        ),
    )
    private val dict = WordDictionary(setOf("kalem", "lam", "elma", "mal", "ela", "ala", "kal"))

    private fun engine() = GameEngine(level, dict)

    @Test
    fun gridWordIsCorrect() {
        val e = engine()
        assertEquals(SubmitResult.Correct("kalem"), e.submit("kalem"))
        assertEquals(1, e.remainingWords)
    }

    @Test
    fun dictionaryWordNotInGridIsBonusOnce() {
        val e = engine()
        assertEquals(SubmitResult.Bonus("elma"), e.submit("elma"))
        assertEquals(1, e.bonusCount)
        assertEquals(SubmitResult.AlreadyFound("elma"), e.submit("elma"))
        assertEquals(1, e.bonusCount)
    }

    @Test
    fun secondSubmitOfGridWordIsAlreadyFound() {
        val e = engine()
        e.submit("lam")
        assertEquals(SubmitResult.AlreadyFound("lam"), e.submit("lam"))
        assertEquals(1, e.remainingWords)
    }

    @Test
    fun unknownWordIsInvalid() {
        assertEquals(SubmitResult.Invalid("kelam"), engine().submit("kelam"))
    }

    @Test
    fun emptyAndBlankAreEmpty() {
        val e = engine()
        assertEquals(SubmitResult.Empty, e.submit(""))
        assertEquals(SubmitResult.Empty, e.submit("   "))
    }

    @Test
    fun inputIsTrimmedAndTurkishLowercased() {
        val e = engine()
        assertEquals(SubmitResult.Correct("kalem"), e.submit("  KALEM "))
    }

    @Test
    fun dottedAndDotlessIAreDifferentWords() {
        val l = Level(1, listOf('ı', 's', 'ı'), listOf(PlacedWord("ısı", 0, 0, true)))
        val d = WordDictionary(setOf("ısı", "isi"))
        val e = GameEngine(l, d)
        // "isi" sözlükte var ama havuzda i yok
        assertEquals(SubmitResult.Invalid("isi"), e.submit("isi"))
        assertEquals(SubmitResult.Invalid("isi"), e.submit("İSİ"))
        assertEquals(SubmitResult.Correct("ısı"), e.submit("ISI"))
    }

    @Test
    fun repeatedLetterNeedsEnoughTilesInPool() {
        // havuzda tek a: "ala" sözlükte olsa da kabul edilmez
        val l = Level(2, listOf('a', 'l'), listOf(PlacedWord("al", 0, 0, true)))
        val e = GameEngine(l, WordDictionary(setOf("al", "ala")))
        assertEquals(SubmitResult.Invalid("ala"), e.submit("ala"))
        // havuzda iki a: kabul (bonus)
        val l2 = Level(3, listOf('a', 'l', 'a'), listOf(PlacedWord("al", 0, 0, true)))
        val e2 = GameEngine(l2, WordDictionary(setOf("al", "ala")))
        assertEquals(SubmitResult.Bonus("ala"), e2.submit("ala"))
    }

    @Test
    fun completesWhenAllGridWordsFound() {
        val e = engine()
        assertFalse(e.isComplete())
        e.submit("kalem")
        assertFalse(e.isComplete())
        e.submit("lam")
        assertTrue(e.isComplete())
        assertEquals(0, e.remainingWords)
    }

    @Test
    fun cellStatesInitially() {
        val e = engine()
        assertEquals(CellState.Hidden, e.cellState(0, 0))
        assertEquals(CellState.Hidden, e.cellState(2, 2))
        assertEquals(CellState.Empty, e.cellState(1, 0))
        assertEquals(CellState.Empty, e.cellState(-1, 0))
        assertEquals(CellState.Empty, e.cellState(50, 50))
    }

    @Test
    fun crossingCellFilledByEitherWord() {
        val e = engine()
        e.submit("lam")
        assertEquals(CellState.Filled, e.cellState(0, 2))
        assertEquals(CellState.Filled, e.cellState(2, 2))
        // kalem'in kesişmeyen kutuları hâlâ kapalı, kesişen kutu görünür
        assertEquals(CellState.Hidden, e.cellState(0, 0))
        assertEquals(CellState.Filled, e.cellState(0, 2))
    }

    @Test
    fun revealLetterOpensHiddenCellUntilNone() {
        val e = engine()
        val total = 5 + 2 // kalem 5 + lam'ın kesişmeyen 2 kutusu
        val seen = mutableSetOf<Pair<Int, Int>>()
        repeat(total) {
            val cell = e.revealLetter()
            assertNotNull(cell)
            assertTrue(seen.add(cell!!))
            assertEquals(CellState.Revealed, e.cellState(cell.first, cell.second))
        }
        assertNull(e.revealLetter())
    }

    @Test
    fun revealLetterSkipsFilledAndRevealedCells() {
        val e = engine()
        e.submit("kalem")
        // kalem bulundu: yalnızca lam'ın (1,2) ve (2,2) kutuları kapalı
        val a = e.revealLetter()!!
        val b = e.revealLetter()!!
        assertEquals(setOf(1 to 2, 2 to 2), setOf(a, b))
        assertNull(e.revealLetter())
    }

    @Test
    fun revealWordMarksUnfoundWordFoundAndShowsCrossings() {
        val e = engine()
        val w = e.revealWord()
        assertNotNull(w)
        assertEquals(1, e.remainingWords)
        val other = if (w!!.word == "kalem") "lam" else "kalem"
        assertEquals(SubmitResult.AlreadyFound(w.word), e.submit(w.word))
        // kesişen kutu diğer kelimede de görünür
        assertEquals(CellState.Filled, e.cellState(0, 2))
        assertNotNull(e.revealWord())
        assertNull(e.revealWord())
        assertTrue(e.isComplete())
        assertEquals(SubmitResult.AlreadyFound(other), e.submit(other))
    }

    @Test
    fun snapshotRestoresEquivalentEngine() {
        val e = engine()
        e.submit("kalem")
        e.submit("elma")
        val cell = e.revealLetter()!!
        val snap = e.snapshot()
        assertEquals(setOf("kalem"), snap.found)
        assertEquals(setOf("elma"), snap.bonusFound)
        assertEquals(setOf(cell), snap.revealed)

        val r = GameEngine(level, dict, snap.found, snap.bonusFound, snap.revealed)
        assertEquals(snap, r.snapshot())
        assertEquals(1, r.remainingWords)
        assertEquals(1, r.bonusCount)
        assertEquals(CellState.Revealed, r.cellState(cell.first, cell.second))
        assertEquals(SubmitResult.AlreadyFound("elma"), r.submit("elma"))
        assertEquals(SubmitResult.AlreadyFound("kalem"), r.submit("kalem"))
    }

    @Test
    fun snapshotIsImmutableCopy() {
        val e = engine()
        val before = e.snapshot()
        e.submit("kalem")
        assertTrue(before.found.isEmpty())
    }
}
