package com.kelimeoyunu.a11y

import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.FeedbackSink
import com.kelimeoyunu.ui.game.GameEvent
import com.kelimeoyunu.ui.game.GameUiState

/**
 * [GameEvent]'leri Türkçe duyuruya çevirip [Announcer]'a verir. Anlık durum [state]'ten okunur.
 * Çift dokunuşla gelen ikinci gönderim (kelime zaten temizlendiği için `Empty`), anlamlı bir
 * sonuçtan sonraki [emptyGraceMs] içinde duyurulmaz.
 */
class AnnouncingFeedbackSink(
    private val announcer: Announcer,
    private val state: () -> GameUiState,
    private val clock: () -> Long,
    private val emptyGraceMs: Long = 800,
) : FeedbackSink {
    private var lastRealResultAt = Long.MIN_VALUE

    override fun onEvent(e: GameEvent) {
        val s = state() as? GameUiState.Ready
        when (e) {
            is GameEvent.LetterAdded -> announcer.say(Announcements.letterAdded(e.char), interrupt = true)
            GameEvent.Deleted -> announcer.say(Announcements.deleted(s?.currentWord.orEmpty()))
            GameEvent.Cleared -> announcer.say(Announcements.cleared())
            is GameEvent.Submitted -> submitted(e.result)
            GameEvent.Shuffled -> announcer.say(Announcements.shuffled(s?.tiles.orEmpty().map { it.char }))
            GameEvent.HintUsed -> announcer.say(Announcements.hintUsed(s?.coins ?: 0))
            GameEvent.NotEnoughCoins -> announcer.say(Announcements.notEnoughCoins())
            GameEvent.NoHintAvailable -> announcer.say(Announcements.noHint())
            is GameEvent.CoinsEarned -> announcer.say(Announcements.coinsEarned(e.amount))
            GameEvent.BonusMeterFull -> announcer.say(Announcements.bonusMeterFull())
            GameEvent.LevelCompleted -> announcer.say(Announcements.levelCompleted(s?.levelNumber ?: 0))
            GameEvent.AllLevelsDone -> announcer.say(Announcements.allDone())
        }
    }

    private fun submitted(r: SubmitResult) {
        val now = clock()
        if (r == SubmitResult.Empty) {
            if (lastRealResultAt != Long.MIN_VALUE && now - lastRealResultAt < emptyGraceMs) return
        } else {
            lastRealResultAt = now
        }
        announcer.say(Announcements.result(r))
    }
}
