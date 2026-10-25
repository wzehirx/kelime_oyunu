package com.kelimeoyunu.audio

import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.GameEvent

enum class Sfx {
    LETTER, DELETE, CLEAR, CORRECT, BONUS, DUPLICATE, INVALID, SHUFFLE, HINT, COIN,
    BONUS_FULL, LEVEL_COMPLETE, NAV, ERROR;

    /** assets/sounds altındaki dosya adı. */
    val fileName: String get() = name.lowercase() + ".wav"
}

/** Olay -> çalınacak sesler (sırayla). Sealed `when` ile yeni olay eklenirse derleme hatası verir. */
fun sfxFor(event: GameEvent): List<Sfx> = listOf(
    when (event) {
        is GameEvent.LetterAdded -> Sfx.LETTER
        GameEvent.Deleted -> Sfx.DELETE
        GameEvent.Cleared -> Sfx.CLEAR
        is GameEvent.Submitted -> when (event.result) {
            is SubmitResult.Correct -> Sfx.CORRECT
            is SubmitResult.Bonus -> Sfx.BONUS
            is SubmitResult.AlreadyFound -> Sfx.DUPLICATE
            is SubmitResult.Invalid -> Sfx.INVALID
            SubmitResult.Empty -> Sfx.ERROR
        }
        GameEvent.Shuffled -> Sfx.SHUFFLE
        GameEvent.HintUsed -> Sfx.HINT
        GameEvent.NotEnoughCoins, GameEvent.NoHintAvailable -> Sfx.ERROR
        is GameEvent.CoinsEarned -> Sfx.COIN
        GameEvent.BonusMeterFull -> Sfx.BONUS_FULL
        GameEvent.LevelCompleted, GameEvent.AllLevelsDone -> Sfx.LEVEL_COMPLETE
    },
)

const val MIN_LETTER_RATE = 0.8f
const val MAX_LETTER_RATE = 1.6f

/** Harf havuzundaki konuma göre perde: soldan sağa 0.8..1.6, monoton artan. Geçersiz girdi uca kenetlenir. */
fun letterPlaybackRate(tileIndex: Int, tileCount: Int): Float {
    if (tileCount <= 1) return 1f
    val t = tileIndex.coerceIn(0, tileCount - 1).toFloat() / (tileCount - 1)
    return MIN_LETTER_RATE + (MAX_LETTER_RATE - MIN_LETTER_RATE) * t
}
