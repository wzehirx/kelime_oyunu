package com.kelimeoyunu.ui.game

/** Compose testleri için sahte durumlar. */
object FakeStates {
    fun ready(
        currentWord: String = "",
        usedIndices: Set<Int> = emptySet(),
        complete: Boolean = false,
    ) = GameUiState.Ready(
        levelNumber = 3,
        tiles = "kalem".mapIndexed { i, c -> LetterTile(i, c, i in usedIndices) },
        currentWord = currentWord,
        coins = 50,
        remainingWords = 2,
        bonusMeter = 2,
        bonusTarget = 5,
        wordRows = listOf(
            WordRow(1, 3, "kal".toList(), found = true),
            WordRow(2, 5, listOf(null, 'a', null, 'e', null), found = false),
            WordRow(3, 4, listOf(null, null, null, null), found = false),
        ),
        levelComplete = complete,
    )
}

/** Çağrıları kaydeden sahte geri çağırımlar. */
class RecordingCallbacks {
    val calls = ArrayList<String>()
    val callbacks = GameCallbacks(
        onLetter = { calls += "letter$it" },
        onSubmit = { calls += "submit" },
        onDelete = { calls += "delete" },
        onClear = { calls += "clear" },
        onShuffle = { calls += "shuffle" },
        onHintLetter = { calls += "hintLetter" },
        onHintWord = { calls += "hintWord" },
        onNextLevel = { calls += "next" },
    )
}
