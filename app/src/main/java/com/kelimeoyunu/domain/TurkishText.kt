package com.kelimeoyunu.domain

import java.util.Locale

object TurkishText {
    private val TR = Locale("tr")

    const val ALPHABET = "abcçdefgğhıijklmnoöprsştuüvyz"

    fun lower(s: String): String = s.lowercase(TR)

    fun upper(s: String): String = s.uppercase(TR)

    /** Kelime, havuzdaki harfler (her biri en fazla bir kez) kullanılarak yazılabilir mi. */
    fun canSpell(word: String, letters: List<Char>): Boolean {
        if (word.isEmpty()) return false
        val available = HashMap<Char, Int>()
        for (c in letters) available.merge(lower(c.toString())[0], 1, Int::plus)
        for (c in lower(word)) {
            val left = available[c] ?: return false
            if (left == 0) return false
            available[c] = left - 1
        }
        return true
    }
}
