package com.kelimeoyunu.ui.game

import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.audio.SfxPlayer
import com.kelimeoyunu.audio.letterPlaybackRate
import com.kelimeoyunu.audio.sfxFor

/** Oyun olaylarını ses efektine çevirir. [tileCount] harf havuzu boyutudur (perde hesabı için). */
class SoundFeedbackSink(
    private val player: SfxPlayer,
    private val gate: DoubleSubmitGate = DoubleSubmitGate(),
    private val tileCount: () -> Int,
) : FeedbackSink {
    override fun onEvent(e: GameEvent) {
        if (gate.shouldSuppress(e)) return
        val rate = if (e is GameEvent.LetterAdded) letterPlaybackRate(e.index, tileCount()) else 1f
        sfxFor(e).forEach { player.play(it, rate) }
    }
}
