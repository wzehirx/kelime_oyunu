package com.kelimeoyunu.ui.game

import com.kelimeoyunu.audio.SfxPlayer
import com.kelimeoyunu.audio.sfxFor

/** Oyun olaylarını ses efektine çevirir. Harf perdesi seçim sırasına bağlıdır ([sfxFor]). */
class SoundFeedbackSink(
    private val player: SfxPlayer,
    private val gate: DoubleSubmitGate = DoubleSubmitGate(),
) : FeedbackSink {
    override fun onEvent(e: GameEvent) {
        if (gate.shouldSuppress(e)) return
        sfxFor(e).forEach { player.play(it, 1f) }
    }
}
