package com.kelimeoyunu.domain

/** [words] küçük harfli (Türkçe kurallarıyla) kelimeler olmalı; fromLines bunu sağlar. */
class WordDictionary(private val words: Set<String>) {
    val size: Int get() = words.size

    fun contains(word: String): Boolean = TurkishText.lower(word.trim()) in words

    fun spellable(letters: List<Char>, min: Int = 2): Set<String> =
        words.filterTo(HashSet()) { it.length >= min && TurkishText.canSpell(it, letters) }

    companion object {
        fun fromLines(lines: Sequence<String>): WordDictionary =
            WordDictionary(
                lines.map { TurkishText.lower(it.trim { c -> c.isWhitespace() || c == '\uFEFF' }) }.filter { it.isNotEmpty() }.toHashSet()
            )
    }
}
