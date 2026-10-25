package com.kelimeoyunu.audio

import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.GameEvent

enum class Sfx {
    LETTER_1, LETTER_2, LETTER_3, LETTER_4, LETTER_5, LETTER_6, LETTER_7,
    DELETE, CLEAR, CORRECT, BONUS, DUPLICATE, INVALID, SHUFFLE, HINT, COIN,
    BONUS_FULL, LEVEL_COMPLETE, NAV, ERROR,
    PLACE_AVRUPA, PLACE_ASYA, PLACE_ORTADOGU, PLACE_AFRIKA, PLACE_AMERIKA, PLACE_OKYANUSYA, PLACE_COMPLETE;

    /** assets/sounds altındaki dosya adı. */
    val fileName: String get() = name.lowercase() + ".wav"
}

/** Olay -> çalınacak sesler (sırayla). Sealed `when` ile yeni olay eklenirse derleme hatası verir. */
fun sfxFor(event: GameEvent): List<Sfx> = listOf(
    when (event) {
        is GameEvent.LetterAdded -> letterSfx(event.position)
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
        is GameEvent.HintUsed -> Sfx.HINT
        GameEvent.NotEnoughCoins, GameEvent.NoHintAvailable -> Sfx.ERROR
        is GameEvent.CoinsEarned -> Sfx.COIN
        GameEvent.BonusMeterFull -> Sfx.BONUS_FULL
        GameEvent.LevelCompleted, GameEvent.AllLevelsDone -> Sfx.LEVEL_COMPLETE
        is GameEvent.PlaceEntered -> placeSfx(event.place.bolge)
        is GameEvent.PlaceCompleted -> Sfx.PLACE_COMPLETE
    },
)

/**
 * Kelimedeki sıraya göre harf sesi: [position] 1 tabanlı (currentWord uzunluğu). 1 en kalın,
 * 7 en ince nota; 1'den küçük LETTER_1'e, 7'den büyük LETTER_7'ye kenetlenir.
 */
fun letterSfx(position: Int): Sfx = when {
    position <= 1 -> Sfx.LETTER_1
    position == 2 -> Sfx.LETTER_2
    position == 3 -> Sfx.LETTER_3
    position == 4 -> Sfx.LETTER_4
    position == 5 -> Sfx.LETTER_5
    position == 6 -> Sfx.LETTER_6
    else -> Sfx.LETTER_7
}

/** Bölge -> giriş melodisi. Yükleyici yalnızca bilinen bölgeleri kabul eder; bilinmeyen için nötr NAV. */
fun placeSfx(bolge: String): Sfx = when (bolge) {
    "avrupa" -> Sfx.PLACE_AVRUPA
    "asya" -> Sfx.PLACE_ASYA
    "ortadogu" -> Sfx.PLACE_ORTADOGU
    "afrika" -> Sfx.PLACE_AFRIKA
    "amerika" -> Sfx.PLACE_AMERIKA
    "okyanusya" -> Sfx.PLACE_OKYANUSYA
    else -> Sfx.NAV
}
