package com.kelimeoyunu.a11y

import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.WordRow
import org.junit.Assert.assertEquals
import org.junit.Test

class AnnouncementsTest {
    @Test fun letterAddedIsUppercaseSingleLetter() {
        assertEquals("K", Announcements.letterAdded('k'))
        assertEquals("İ", Announcements.letterAdded('i'))
        assertEquals("I", Announcements.letterAdded('ı'))
    }

    @Test fun deleted() {
        assertEquals("Silindi, kalan: KA", Announcements.deleted("ka"))
        assertEquals("Kelime boş", Announcements.deleted(""))
    }

    @Test fun cleared() = assertEquals("Temizlendi", Announcements.cleared())

    @Test fun results() {
        assertEquals("Doğru, KALEM", Announcements.result(SubmitResult.Correct("kalem")))
        assertEquals("Bonus kelime, KALEM", Announcements.result(SubmitResult.Bonus("kalem")))
        assertEquals("Bu kelime zaten bulundu", Announcements.result(SubmitResult.AlreadyFound("kalem")))
        assertEquals("Geçersiz kelime", Announcements.result(SubmitResult.Invalid("xyz")))
        assertEquals("Önce harf seç", Announcements.result(SubmitResult.Empty))
    }

    @Test fun resultUsesTurkishUppercase() {
        assertEquals("Doğru, IŞIK", Announcements.result(SubmitResult.Correct("ışık")))
        assertEquals("Doğru, İZMİR", Announcements.result(SubmitResult.Correct("izmir")))
    }

    @Test fun shuffled() =
        assertEquals("Harfler karıştırıldı: K, A, L, E, M", Announcements.shuffled(listOf('k', 'a', 'l', 'e', 'm')))

    @Test fun hintAndCoins() {
        assertEquals("İpucu kullanıldı, kalan jeton: 40", Announcements.hintUsed(40))
        assertEquals("Yeterli jeton yok", Announcements.notEnoughCoins())
        assertEquals("Açılacak ipucu yok", Announcements.noHint())
        assertEquals("10 jeton kazandın", Announcements.coinsEarned(10))
        assertEquals("Bonus sayacı doldu", Announcements.bonusMeterFull())
    }

    @Test fun levelEvents() {
        assertEquals("Seviye 3 tamamlandı", Announcements.levelCompleted(3))
        assertEquals("Tebrikler, tüm seviyeleri tamamladın", Announcements.allDone())
    }

    @Test fun rowNotFoundShowsOpenLetters() {
        val row = WordRow(2, 5, listOf(null, 'a', null, 'e', null), found = false)
        assertEquals("Kelime 2, 5 harfli, açık harfler: _ A _ E _", Announcements.wordRowDescription(row))
    }

    @Test fun rowFoundShowsWord() {
        val row = WordRow(2, 5, "kalem".toList(), found = true)
        assertEquals("Kelime 2, 5 harfli, bulundu: KALEM", Announcements.wordRowDescription(row))
    }

    @Test fun rowFullyHiddenAndTurkishCase() {
        assertEquals("Kelime 1, 3 harfli, açık harfler: _ _ _", Announcements.wordRowDescription(WordRow(1, 3, listOf(null, null, null), false)))
        assertEquals("Kelime 1, 3 harfli, bulundu: IŞI", Announcements.wordRowDescription(WordRow(1, 3, "ışı".toList(), true)))
    }

    @Test fun currentWord() {
        assertEquals("Oluşturulan kelime: KAL", Announcements.currentWordDescription("kal"))
        assertEquals("Oluşturulan kelime: boş", Announcements.currentWordDescription(""))
    }

    @Test fun letterButton() {
        assertEquals("Harf K", Announcements.letterButton('k', selected = false))
        assertEquals("Harf K, seçildi", Announcements.letterButton('k', selected = true))
    }

    @Test fun header() {
        assertEquals(
            "Seviye 3. 50 jeton. Kalan kelime: 4. Bonus sayacı: 2 / 5",
            Announcements.headerDescription(3, 50, 2, 5, 4),
        )
    }
}
