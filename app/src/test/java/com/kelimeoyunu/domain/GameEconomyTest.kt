package com.kelimeoyunu.domain

import com.kelimeoyunu.data.START_COINS
import com.kelimeoyunu.ui.game.GameViewModel
import com.kelimeoyunu.ui.menu.HowToPlayParagraphs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.4 jeton dengesi: değerler tek yerde (GameEconomy) ve metinler oradan üretilir. */
class GameEconomyTest {
    @Test
    fun v14Values() {
        assertEquals(50, GameEconomy.START_COINS)
        assertEquals(5, GameEconomy.LEVEL_REWARD)
        assertEquals(10, GameEconomy.BONUS_REWARD)
        assertEquals(5, GameEconomy.BONUS_TARGET)
        assertEquals(25, GameEconomy.PLACE_REWARD)
        assertEquals(15, GameEconomy.HINT_LETTER_COST)
        assertEquals(40, GameEconomy.HINT_WORD_COST)
    }

    @Test
    fun otherDeclarationsAreAliasesOfTheSingleSource() {
        assertEquals(GameEconomy.START_COINS, START_COINS)
        assertEquals(GameEconomy.BONUS_REWARD, GameViewModel.BONUS_REWARD)
        assertEquals(GameEconomy.LEVEL_REWARD, GameViewModel.LEVEL_REWARD)
        assertEquals(GameEconomy.PLACE_REWARD, GameViewModel.PLACE_REWARD)
        assertEquals(GameEconomy.HINT_LETTER_COST, GameViewModel.HINT_LETTER_COST)
        assertEquals(GameEconomy.HINT_WORD_COST, GameViewModel.HINT_WORD_COST)
        assertEquals(GameEconomy.BONUS_TARGET, GameViewModel.BONUS_TARGET)
    }

    @Test
    fun buttonTextsCarryTheCosts() {
        assertEquals("İpucu, harf aç, 15 jeton", GameEconomy.HINT_LETTER_DESCRIPTION)
        assertEquals("İpucu: harf aç, 15 jeton", GameEconomy.HINT_LETTER_LABEL)
        assertEquals("Kelime aç, 40 jeton", GameEconomy.HINT_WORD_DESCRIPTION)
        assertEquals("Kelime aç, 40 jeton", GameEconomy.HINT_WORD_LABEL)
    }

    @Test
    fun howToPlayTextMatchesTheConstants() {
        val text = HowToPlayParagraphs.joinToString(" ")
        assertTrue(text, "Her 5 bonus kelime 10 jeton kazandırır" in text)
        assertTrue(text, "Seviyeyi bitirince 5 jeton kazanırsın" in text)
        assertTrue(text, "İpucu düğmesi 15 jetonla bir harf" in text)
        assertTrue(text, "Kelime aç düğmesi 40 jetonla" in text)
        assertTrue(text, "ülkeyi bitirince 25 jeton" in text)
        assertTrue(text, "Başlangıçta 50 jetonun var" in text)
    }
}
