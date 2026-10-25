package com.kelimeoyunu.ui.game

import com.kelimeoyunu.domain.SubmitResult

/** [index] harf havuzundaki kimliktir (karıştırınca değişmez); [tiles] listesi gösterim sırasındadır. */
data class LetterTile(val index: Int, val char: Char, val used: Boolean)

/** [cells]: açık harf ya da gizliyse null. */
data class WordRow(val number: Int, val length: Int, val cells: List<Char?>, val found: Boolean)

sealed interface GameUiState {
    data object Loading : GameUiState
    data class Error(val message: String) : GameUiState
    data object AllDone : GameUiState
    data class Ready(
        val levelNumber: Int,
        val tiles: List<LetterTile>,
        val currentWord: String,
        val coins: Int,
        val remainingWords: Int,
        val bonusMeter: Int,
        val bonusTarget: Int,
        val wordRows: List<WordRow>,
        val levelComplete: Boolean,
    ) : GameUiState
}

sealed interface GameEvent {
    data class LetterAdded(val char: Char, val index: Int, val position: Int) : GameEvent
    data object Deleted : GameEvent
    data object Cleared : GameEvent
    data class Submitted(val result: SubmitResult) : GameEvent
    data object Shuffled : GameEvent
    /** [coinsLeft]: ipucu bedeli düşüldükten sonraki bakiye (sonradan gelen ödüller dahil değil). */
    data class HintUsed(val coinsLeft: Int) : GameEvent
    data object NotEnoughCoins : GameEvent
    data object NoHintAvailable : GameEvent
    data class CoinsEarned(val amount: Int) : GameEvent
    data object BonusMeterFull : GameEvent
    data object LevelCompleted : GameEvent
    data object AllLevelsDone : GameEvent
}
