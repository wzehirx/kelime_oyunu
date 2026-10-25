package com.kelimeoyunu.a11y

import com.kelimeoyunu.TestPlaces
import com.kelimeoyunu.domain.PlacePosition
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

    private val tr = TestPlaces.turkey
    private val pos = PlacePosition(tr, 0, 3, 20)

    @Test fun headerWithPlaceKeepsAllOldInfo() {
        assertEquals(
            "Seviye 3, Türkiye 3 / 20. 50 jeton. Kalan kelime: 4. Bonus sayacı: 2 / 5",
            Announcements.headerDescription(3, 50, 2, 5, 4, pos),
        )
        assertEquals("Türkiye 3 / 20", Announcements.placeProgress(pos))
    }

    @Test fun continueLabelNamesPlace() {
        assertEquals("Devam et, Türkiye, 3 / 20", Announcements.continueLabel(pos))
    }

    @Test fun placeEntered() {
        assertEquals(
            "Yeni ülke: Türkiye, Asya. Başkenti Ankara. 20 seviyelik yolculuk başlıyor. " +
                "Biliyor muydun? Türkiye iki kıtada yer alır.",
            Announcements.placeEntered(tr, 20),
        )
    }

    @Test fun placeCompletedWithAndWithoutNext() {
        assertEquals(
            "Türkiye tamamlandı! 25 jeton kazandın. Biliyor muydun? Kapadokya'da peri bacaları vardır. Sıradaki ülke: Yunanistan.",
            Announcements.placeCompleted(tr, 25, "Yunanistan"),
        )
        assertEquals(
            "Türkiye tamamlandı! 25 jeton kazandın. Biliyor muydun? Kapadokya'da peri bacaları vardır.",
            Announcements.placeCompleted(tr, 25, null),
        )
    }

    @Test fun worldTourTexts() {
        assertEquals("Dünya turu, 3 / 100 ülke tamamlandı", Announcements.placesTitle(3, 100))
        assertEquals("Türkiye, kilitli", Announcements.placeLocked(tr))
        assertEquals("Türkiye, 4 / 20 seviye, şu an buradasın", Announcements.placeInProgress(tr, 4, 20))
        assertEquals(
            "Türkiye, tamamlandı. Başkenti Ankara. Türkiye iki kıtada yer alır. Kapadokya'da peri bacaları vardır.",
            Announcements.placeCompletedRow(tr),
        )
        assertEquals("Henüz bu ülkeye ulaşmadın", Announcements.placeNotReached())
    }
}
