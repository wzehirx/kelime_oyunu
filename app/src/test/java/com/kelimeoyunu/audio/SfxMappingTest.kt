package com.kelimeoyunu.audio

import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.GameEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SfxMappingTest {
    private val cases: List<Pair<GameEvent, List<Sfx>>> = listOf(
        GameEvent.LetterAdded('A', 0, 1) to listOf(Sfx.LETTER_1),
        GameEvent.LetterAdded('A', 5, 4) to listOf(Sfx.LETTER_4),
        GameEvent.Deleted to listOf(Sfx.DELETE),
        GameEvent.Cleared to listOf(Sfx.CLEAR),
        GameEvent.Submitted(SubmitResult.Correct("ev")) to listOf(Sfx.CORRECT),
        GameEvent.Submitted(SubmitResult.Bonus("ev")) to listOf(Sfx.BONUS),
        GameEvent.Submitted(SubmitResult.AlreadyFound("ev")) to listOf(Sfx.DUPLICATE),
        GameEvent.Submitted(SubmitResult.Invalid("xx")) to listOf(Sfx.INVALID),
        GameEvent.Submitted(SubmitResult.Empty) to listOf(Sfx.ERROR),
        GameEvent.Shuffled to listOf(Sfx.SHUFFLE),
        GameEvent.HintUsed(0) to listOf(Sfx.HINT),
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
        assertEquals("letter_1.wav", Sfx.LETTER_1.fileName)
        assertEquals("letter_7.wav", Sfx.LETTER_7.fileName)
        assertEquals(20, Sfx.entries.size)
    }

    @Test fun letterSfxFollowsPositionAndClamps() {
        assertEquals(Sfx.LETTER_1, letterSfx(1))
        assertEquals(Sfx.LETTER_3, letterSfx(3))
        assertEquals(Sfx.LETTER_7, letterSfx(7))
        assertEquals(Sfx.LETTER_7, letterSfx(8))
        assertEquals(Sfx.LETTER_1, letterSfx(0))
        assertEquals(Sfx.LETTER_1, letterSfx(-5))
        assertEquals(Sfx.LETTER_7, letterSfx(Int.MAX_VALUE))
        val all = (1..7).map(::letterSfx)
        assertEquals(7, all.toSet().size)
        assertEquals((1..7).map { "letter_$it.wav" }, all.map { it.fileName })
    }
}
