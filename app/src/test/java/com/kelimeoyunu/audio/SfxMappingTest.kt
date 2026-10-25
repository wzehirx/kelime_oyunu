package com.kelimeoyunu.audio

import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.GameEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SfxMappingTest {
    private val cases: List<Pair<GameEvent, List<Sfx>>> = listOf(
        GameEvent.LetterAdded('A', 0) to listOf(Sfx.LETTER),
        GameEvent.Deleted to listOf(Sfx.DELETE),
        GameEvent.Cleared to listOf(Sfx.CLEAR),
        GameEvent.Submitted(SubmitResult.Correct("ev")) to listOf(Sfx.CORRECT),
        GameEvent.Submitted(SubmitResult.Bonus("ev")) to listOf(Sfx.BONUS),
        GameEvent.Submitted(SubmitResult.AlreadyFound("ev")) to listOf(Sfx.DUPLICATE),
        GameEvent.Submitted(SubmitResult.Invalid("xx")) to listOf(Sfx.INVALID),
        GameEvent.Submitted(SubmitResult.Empty) to listOf(Sfx.ERROR),
        GameEvent.Shuffled to listOf(Sfx.SHUFFLE),
        GameEvent.HintUsed to listOf(Sfx.HINT),
        GameEvent.NotEnoughCoins to listOf(Sfx.ERROR),
        GameEvent.NoHintAvailable to listOf(Sfx.ERROR),
        GameEvent.CoinsEarned(3) to listOf(Sfx.COIN),
        GameEvent.BonusMeterFull to listOf(Sfx.BONUS_FULL),
        GameEvent.LevelCompleted to listOf(Sfx.LEVEL_COMPLETE),
        GameEvent.AllLevelsDone to listOf(Sfx.LEVEL_COMPLETE),
    )

    @Test fun everyEventMapsAsSpecified() {
        for ((event, expected) in cases) assertEquals("$event", expected, sfxFor(event))
    }

    @Test fun everyGameEventSubclassIsCovered() {
        // kotlin-reflect yok: iç içe sınıflar Java yansımasıyla bulunur.
        val covered = cases.map { it.first::class.java }.toSet()
        val all = GameEvent::class.java.classes.filter { GameEvent::class.java.isAssignableFrom(it) }.toSet()
        assertTrue(all.size >= 12)
        assertEquals(all, covered)
    }

    @Test fun fileNameIsLowercaseNamePlusWav() {
        assertEquals("bonus_full.wav", Sfx.BONUS_FULL.fileName)
        assertEquals("letter.wav", Sfx.LETTER.fileName)
        assertEquals(14, Sfx.entries.size)
    }

    @Test fun letterRateIsMonotonicWithinRange() {
        for (count in 2..12) {
            val rates = (0 until count).map { letterPlaybackRate(it, count) }
            assertEquals(0.8f, rates.first(), 1e-6f)
            assertEquals(1.6f, rates.last(), 1e-6f)
            rates.zipWithNext().forEach { (a, b) -> assertTrue("$count $rates", b > a) }
        }
    }

    @Test fun letterRateDegenerateInputsStayInRange() {
        for ((i, n) in listOf(0 to 0, 0 to 1, -3 to 7, 99 to 7, 5 to -2)) {
            val r = letterPlaybackRate(i, n)
            assertTrue("$i/$n -> $r", r in 0.8f..1.6f)
        }
    }
}
