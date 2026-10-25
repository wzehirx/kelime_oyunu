package com.kelimeoyunu.a11y

import com.kelimeoyunu.ui.game.DoubleSubmitGate
import com.kelimeoyunu.ui.game.FeedbackSink
import com.kelimeoyunu.ui.game.GameEvent
import com.kelimeoyunu.ui.game.GameUiState

/**
 * [GameEvent]'leri Türkçe duyuruya çevirip [Announcer]'a verir. Anlık durum [state]'ten okunur (ipucu bakiyesi hariç: olay taşır).
 * Çift dokunuşla gelen ikinci gönderim (kelime zaten temizlendiği için `Empty`), anlamlı bir
 * sonuçtan sonraki [emptyGraceMs] içinde duyurulmaz. Ses açıksa ülke girişi duyurusu [placeDelayMs] gecikir
 * (önce bölge melodisi çalar, konuşma onunla çakışmaz); ses kapalıysa hemen söylenir.
 */
class AnnouncingFeedbackSink(
    private val announcer: Announcer,
    private val state: () -> GameUiState,
    clock: () -> Long,
    emptyGraceMs: Long = 800,
    private val soundOn: () -> Boolean = { false },
    private val placeDelayMs: Long = 1000,
) : FeedbackSink {
    private val gate = DoubleSubmitGate(clock, emptyGraceMs)

    override fun onEvent(e: GameEvent) {
        if (gate.shouldSuppress(e)) return
        val s = state() as? GameUiState.Ready
        when (e) {
            is GameEvent.LetterAdded -> announcer.say(Announcements.letterAdded(e.char), interrupt = true)
            GameEvent.Deleted -> announcer.say(Announcements.deleted(s?.currentWord.orEmpty()))
            GameEvent.Cleared -> announcer.say(Announcements.cleared())
            is GameEvent.Submitted -> announcer.say(Announcements.result(e.result))
            GameEvent.Shuffled -> announcer.say(Announcements.shuffled(s?.tiles.orEmpty().map { it.char }))
            is GameEvent.HintUsed -> announcer.say(Announcements.hintUsed(e.coinsLeft))
            GameEvent.NotEnoughCoins -> announcer.say(Announcements.notEnoughCoins())
            GameEvent.NoHintAvailable -> announcer.say(Announcements.noHint())
            is GameEvent.CoinsEarned -> announcer.say(Announcements.coinsEarned(e.amount))
            GameEvent.BonusMeterFull -> announcer.say(Announcements.bonusMeterFull())
            GameEvent.LevelCompleted -> announcer.say(Announcements.levelCompleted(s?.levelNumber ?: 0))
            is GameEvent.PlaceEntered -> announcer.say(
                Announcements.placeEntered(e.place, e.placeLevelCount),
                delayMs = if (soundOn()) placeDelayMs else 0,
            )
            is GameEvent.PlaceCompleted -> announcer.say(
                Announcements.placeCompleted(e.place, e.bonusCoins, e.nextPlaceName),
            )
            GameEvent.AllLevelsDone -> announcer.say(Announcements.allDone())
        }
    }
}
