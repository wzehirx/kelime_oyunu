package com.kelimeoyunu.ui.game

import com.kelimeoyunu.audio.FakeSoundBackend
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.audio.SoundManager
import com.kelimeoyunu.audio.letterPlaybackRate
import com.kelimeoyunu.domain.SubmitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundFeedbackSinkTest {
    private val backend = FakeSoundBackend()
    private val manager = SoundManager({ backend }).also { it.start(); it.enabled = true }
    private fun sink(tiles: Int = 6) = SoundFeedbackSink(manager) { tiles }

    @Test fun letterUsesTileIndexPitch() {
        sink(6).onEvent(GameEvent.LetterAdded('K', 3))
        assertEquals(listOf(Sfx.LETTER to letterPlaybackRate(3, 6)), backend.played)
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
            GameEvent.LetterAdded('A', 0), GameEvent.Deleted, GameEvent.LevelCompleted,
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
}
