package com.kelimeoyunu.domain

data class PlacedWord(val word: String, val row: Int, val col: Int, val horizontal: Boolean) {
    /** Kelimenin kapladığı (satır, sütun) kutuları, harf sırasıyla. */
    fun cells(): List<Pair<Int, Int>> =
        word.indices.map { i -> if (horizontal) row to col + i else row + i to col }
}

data class Level(val id: Int, val letters: List<Char>, val words: List<PlacedWord>)

sealed interface SubmitResult {
    data class Correct(val word: String) : SubmitResult
    data class Bonus(val word: String) : SubmitResult
    data class AlreadyFound(val word: String) : SubmitResult
    data class Invalid(val word: String) : SubmitResult
    data object Empty : SubmitResult
}

enum class CellState { Hidden, Revealed, Filled, Empty }

/** Kalıcılık için motor durumu. */
data class EngineState(
    val found: Set<String>,
    val bonusFound: Set<String>,
    val revealed: Set<Pair<Int, Int>>,
)
