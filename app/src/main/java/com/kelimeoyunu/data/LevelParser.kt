package com.kelimeoyunu.data

import com.kelimeoyunu.domain.Level
import com.kelimeoyunu.domain.PlacedWord
import com.kelimeoyunu.domain.TurkishText

/** `id|harfler|kelime,satır,sütun,y;kelime,satır,sütun,d;...` satırını okur. Bozuksa null. */
internal object LevelParser {
    fun parse(line: String): Level? {
        val parts = line.trim().split('|')
        if (parts.size != 3) return null
        val id = parts[0].toIntOrNull() ?: return null
        val letters = TurkishText.lower(parts[1]).toList()
        if (letters.isEmpty()) return null
        val words = parts[2].split(';').map { parseWord(it) ?: return null }
        if (words.isEmpty()) return null
        if (!words.all { TurkishText.canSpell(it.word, letters) }) return null
        if (!crossingsAgree(words)) return null
        return Level(id, letters, words)
    }

    private fun parseWord(s: String): PlacedWord? {
        val f = s.split(',')
        if (f.size != 4 || f[0].isEmpty()) return null
        val row = f[1].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val col = f[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val horizontal = when (f[3]) {
            "y" -> true
            "d" -> false
            else -> return null
        }
        return PlacedWord(TurkishText.lower(f[0]), row, col, horizontal)
    }

    private fun crossingsAgree(words: List<PlacedWord>): Boolean {
        val grid = HashMap<Pair<Int, Int>, Char>()
        for (w in words) {
            w.cells().forEachIndexed { i, cell ->
                if (grid.getOrPut(cell) { w.word[i] } != w.word[i]) return false
            }
        }
        return true
    }
}
