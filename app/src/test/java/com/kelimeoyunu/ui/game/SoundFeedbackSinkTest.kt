package com.kelimeoyunu.ui.game

import com.kelimeoyunu.audio.FakeSoundBackend
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.audio.SoundManager
import com.kelimeoyunu.domain.SubmitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundFeedbackSinkTest {
    private val backend = FakeSoundBackend()
    private val manager = SoundManager({ backend }).also { it.start(); it.enabled = true }
    private fun sink() = SoundFeedbackSink(manager)

    @Test fun letterSoundDependsOnPositionNotTileIndex() {
        val s = sink()
        s.onEvent(GameEvent.LetterAdded('K', 5, 1))
        s.onEvent(GameEvent.LetterAdded('K', 0, 3))
        s.onEvent(GameEvent.LetterAdded('K', 2, 9))
        assertEquals(listOf(Sfx.LETTER_1 to 1f, Sfx.LETTER_3 to 1f, Sfx.LETTER_7 to 1f), backend.played)
    }

    @Test fun correctThenCoinsPlayInOrder() {
        val s = sink()
        s.onEvent(GameEvent.Submitted(SubmitResult.Correct("ev")))
        s.onEvent(GameEvent.CoinsEarned(1))
        assertEquals(listOf(Sfx.CORRECT, Sfx.COIN), backend.played.map { it.first })
        assertTrue(backend.played.all { it.second == 1f })
    }

    @Test fun soundOffPlaysNothing() {
        manager.enabled = false
        val s = sink()
        listOf(
            GameEvent.LetterAdded('A', 0, 1), GameEvent.Deleted, GameEvent.LevelCompleted(),
            GameEvent.CoinsEarned(2), GameEvent.NotEnoughCoins,
        ).forEach(s::onEvent)
        assertTrue(backend.played.isEmpty())
    }

    @Test fun worksInsideComposite() {
        val seen = mutableListOf<GameEvent>()
        CompositeFeedbackSink(FeedbackSink { seen += it }, sink()).onEvent(GameEvent.Shuffled)
        assertEquals(1, seen.size)
        assertEquals(listOf(Sfx.SHUFFLE), backend.played.map { it.first })
    }

    @Test fun regionMelodiesAndPlaceComplete() {
        val s = sink()
        val regions = listOf(
            "avrupa" to Sfx.PLACE_AVRUPA, "asya" to Sfx.PLACE_ASYA, "ortadogu" to Sfx.PLACE_ORTADOGU,
            "afrika" to Sfx.PLACE_AFRIKA, "amerika" to Sfx.PLACE_AMERIKA, "okyanusya" to Sfx.PLACE_OKYANUSYA,
        )
        for ((bolge, _) in regions) {
            s.onEvent(GameEvent.PlaceEntered(com.kelimeoyunu.TestPlaces.turkey.copy(bolge = bolge), 20))
        }
        s.onEvent(GameEvent.PlaceCompleted(com.kelimeoyunu.TestPlaces.turkey, 25, null))
        assertEquals(regions.map { it.second } + Sfx.PLACE_COMPLETE, backend.played.map { it.first })
    }

    @Test fun placeSoundsAreSilentWhenSoundIsOff() {
        manager.enabled = false
        val s = sink()
        s.onEvent(GameEvent.PlaceEntered(com.kelimeoyunu.TestPlaces.turkey, 20))
        s.onEvent(GameEvent.PlaceCompleted(com.kelimeoyunu.TestPlaces.turkey, 25, null))
        assertTrue(backend.played.isEmpty())
    }
}
