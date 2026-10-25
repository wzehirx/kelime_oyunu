package com.kelimeoyunu.domain

import kotlin.random.Random

class GameEngine(
    private val level: Level,
    private val dictionary: WordDictionary,
    found: Set<String> = emptySet(),
    bonusFound: Set<String> = emptySet(),
    revealed: Set<Pair<Int, Int>> = emptySet(),
    private val random: Random = Random.Default,
) {
    private val found = found.toMutableSet()
    private val bonusFound = bonusFound.toMutableSet()
    private val revealed = revealed.toMutableSet()

    /** Izgaradaki her kutunun harfi (kesişen kutular aynı harfi taşır). */
    private val gridCells: Map<Pair<Int, Int>, Char> = buildMap {
        for (w in level.words) w.cells().forEachIndexed { i, cell -> put(cell, w.word[i]) }
    }
    private val gridWords: Map<String, PlacedWord> = level.words.associateBy { it.word }

    val remainingWords: Int get() = gridWords.keys.count { it !in found }
    val bonusCount: Int get() = bonusFound.size

    fun isComplete(): Boolean = remainingWords == 0

    fun submit(raw: String): SubmitResult {
        val word = TurkishText.lower(raw.trim())
        if (word.isEmpty()) return SubmitResult.Empty
        if (word in found || word in bonusFound) return SubmitResult.AlreadyFound(word)
        if (!TurkishText.canSpell(word, level.letters)) return SubmitResult.Invalid(word)
        return when {
            word in gridWords -> {
                found += word
                SubmitResult.Correct(word)
            }
            dictionary.contains(word) -> {
                bonusFound += word
                SubmitResult.Bonus(word)
            }
            else -> SubmitResult.Invalid(word)
        }
    }

    fun cellState(row: Int, col: Int): CellState {
        val cell = row to col
        if (cell !in gridCells) return CellState.Empty
        if (level.words.any { it.word in found && cell in it.cells() }) return CellState.Filled
        return if (cell in revealed) CellState.Revealed else CellState.Hidden
    }

    /** Henüz görünmeyen (bulunmamış ve açılmamış) bir kutuyu açar; yoksa null. */
    fun revealLetter(): Pair<Int, Int>? {
        val hidden = gridCells.keys.filter { cellState(it.first, it.second) == CellState.Hidden }
        if (hidden.isEmpty()) return null
        return hidden[random.nextInt(hidden.size)].also { revealed += it }
    }

    /** Bulunmamış bir kelimeyi bulunmuş sayar; yoksa null. */
    fun revealWord(): PlacedWord? {
        val open = level.words.filter { it.word !in found }
        if (open.isEmpty()) return null
        return open[random.nextInt(open.size)].also { found += it.word }
    }

    fun snapshot(): EngineState = EngineState(found.toSet(), bonusFound.toSet(), revealed.toSet())
}
