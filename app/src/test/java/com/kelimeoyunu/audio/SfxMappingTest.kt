package com.kelimeoyunu.audio

import com.kelimeoyunu.domain.Place
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
        GameEvent.PlaceEntered(place("avrupa"), 20) to listOf(Sfx.PLACE_AVRUPA),
        GameEvent.PlaceCompleted(place("avrupa"), 25, null) to listOf(Sfx.PLACE_COMPLETE),
    )

    private fun place(bolge: String) = Place("x", "Ülke", "Asya", bolge, "Başkent", "a", "b")

    @Test fun everyRegionHasItsOwnMelody() {
        val expected = mapOf(
            "avrupa" to Sfx.PLACE_AVRUPA, "asya" to Sfx.PLACE_ASYA, "ortadogu" to Sfx.PLACE_ORTADOGU,
            "afrika" to Sfx.PLACE_AFRIKA, "amerika" to Sfx.PLACE_AMERIKA, "okyanusya" to Sfx.PLACE_OKYANUSYA,
        )
        for ((bolge, sfx) in expected) assertEquals(bolge, listOf(sfx), sfxFor(GameEvent.PlaceEntered(place(bolge), 50)))
        assertEquals(6, expected.values.toSet().size)
        assertEquals("place_ortadogu.wav", Sfx.PLACE_ORTADOGU.fileName)
        assertEquals("place_complete.wav", Sfx.PLACE_COMPLETE.fileName)
        assertEquals(listOf(Sfx.NAV), sfxFor(GameEvent.PlaceEntered(place("bilinmeyen"), 5)))
    }

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
        assertEquals(27, Sfx.entries.size)
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
