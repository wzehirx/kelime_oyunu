package com.kelimeoyunu.a11y

import com.kelimeoyunu.TestAssets
import com.kelimeoyunu.TestPlaces
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.audio.sfxFor
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.CompositeFeedbackSink
import com.kelimeoyunu.ui.game.GameEvent
import com.kelimeoyunu.ui.game.GameUiState
import com.kelimeoyunu.ui.game.LetterTile
import com.kelimeoyunu.ui.game.SoundFeedbackSink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** QA: ülke duyuruları (gecikme, sıralama, birleşik metin) ve ses/duyuru zamanlaması. */
@OptIn(ExperimentalCoroutinesApi::class)
class WorldTourAnnouncementAdversarialTest {
    private val spoken = ArrayList<Pair<Long, String>>()
    private val place = TestPlaces.turkey
    private var now = 0L
    private var soundOn = true

    private fun ready(word: String = "", n: Int = 20) = GameUiState.Ready(
        levelNumber = n, tiles = "kalem".mapIndexed { i, c -> LetterTile(i, c, false) }, currentWord = word,
        coins = 40, remainingWords = 0, bonusMeter = 0, bonusTarget = 5, wordRows = emptyList(), levelComplete = true,
    )

    private fun TestScope.sink() = AnnouncingFeedbackSink(
        Announcer({ spoken += currentTime to it }, backgroundScope), { ready() }, { now }, soundOn = { soundOn },
    )

    @Test fun placeEntryWithSoundWaitsForMelodyThenSpeaks() = runTest {
        val s = sink()
        s.onEvent(GameEvent.PlaceEntered(place, 20))
        advanceTimeBy(PLACE_INTRO_DELAY_MS); runCurrent()
        assertTrue(spoken.isEmpty())
        advanceTimeBy(130); runCurrent()
        assertEquals(1, spoken.size)
        assertTrue(spoken[0].second.startsWith("Yeni ülke: Türkiye, Asya. Başkenti Ankara. 20 seviyelik yolculuk başlıyor. Biliyor muydun?"))
    }

    @Test fun placeEntryWithSoundOffIsImmediateWithoutExtraDelay() = runTest {
        soundOn = false
        val s = sink()
        s.onEvent(GameEvent.PlaceEntered(place, 20))
        advanceTimeBy(121); runCurrent()
        assertEquals(1, spoken.size)
    }

    @Test fun letterTapDuringEntryDelayDoesNotCrushTheCountryAnnouncementCharacterization() = runTest {
        val s = sink()
        s.onEvent(GameEvent.PlaceEntered(place, 20))
        advanceTimeBy(300)
        s.onEvent(GameEvent.LetterAdded('t', 0, 1)); advanceTimeBy(130); runCurrent()
        s.onEvent(GameEvent.LetterAdded('e', 1, 2)); advanceTimeBy(130); runCurrent()
        advanceTimeBy(PLACE_INTRO_DELAY_MS); runCurrent()
        // harf duyuruları sırayla, ülke duyurusu SONRA ve eksiksiz gelir (kaybolmaz, ezilmez)
        assertEquals(listOf("T", "E"), spoken.take(2).map { it.second })
        assertTrue(spoken.last().second.startsWith("Yeni ülke: Türkiye"))
        assertEquals(3, spoken.size)
    }

    @Test fun letterArrivingExactlyWhenDelayedCountryAnnouncementIsDueIsMergedInOneSentence() = runTest {
        val s = sink()
        s.onEvent(GameEvent.PlaceEntered(place, 20))
        advanceTimeBy(PLACE_INTRO_DELAY_MS + 50)
        s.onEvent(GameEvent.LetterAdded('t', 0, 1))
        advanceTimeBy(130); runCurrent()
        // harf, ülke metninden sonra aynı cümleye girer
        assertEquals(1, spoken.size)
        assertTrue(spoken[0].second, spoken[0].second.startsWith("Yeni ülke") && spoken[0].second.endsWith(". T"))
    }

    @Test fun lastLevelOfCountryAnnouncementIsOneReadableSentence() = runTest {
        val s = sink()
        s.onEvent(GameEvent.Submitted(SubmitResult.Correct("net")))
        s.onEvent(GameEvent.CoinsEarned(10, silent = true))
        s.onEvent(GameEvent.LevelCompleted(placeCompleted = true))
        s.onEvent(GameEvent.PlaceCompleted(place, 25, "Yunanistan"))
        advanceTimeBy(130); runCurrent()
        assertEquals(1, spoken.size)
        val text = spoken[0].second
        assertEquals(
            "Doğru, NET. 10 jeton kazandın. Seviye 20 tamamlandı. Türkiye tamamlandı! 25 jeton kazandın. " +
                "Biliyor muydun? Kapadokya'da peri bacaları vardır. Sıradaki ülke: Yunanistan",
            text,
        )
        assertFalse(text.contains(".."))
        assertFalse(text.contains(" ."))
    }

    @Test fun lastCountryHasNoNextCountrySentence() = runTest {
        val s = sink()
        s.onEvent(GameEvent.PlaceCompleted(place, 25, null))
        advanceTimeBy(130); runCurrent()
        assertFalse(spoken[0].second.contains("Sıradaki"))
    }

    @Test fun realPlacesInfoSentencesStaySpeakableLength() {
        val places = com.kelimeoyunu.data.loadPlaces { TestAssets.open("places.json") }
        var longest = 0
        for (p in places) {
            val entered = Announcements.placeEntered(p, 138)
            val completed = Announcements.placeCompleted(p, 25, p.ad)
            longest = maxOf(longest, entered.length, completed.length)
            assertFalse(entered.contains("  "))
        }
        // 3 olay birleşince ~300 karakteri aşmasın (≈ 20 sn konuşma)
        assertTrue("en uzun duyuru $longest karakter", longest < 300)
    }

    // ---- ses ----

    private class Recorder : com.kelimeoyunu.audio.SfxPlayer {
        val played = ArrayList<Sfx>()
        override fun play(sfx: Sfx, rate: Float) { played += sfx }
    }

    @Test fun BUG_levelCompleteAndPlaceCompleteJinglesAreNotStackedOnTopOfEachOther() {
        val rec = Recorder()
        val sink = SoundFeedbackSink(rec)
        listOf(
            GameEvent.Submitted(SubmitResult.Correct("net")), GameEvent.CoinsEarned(10, silent = true), GameEvent.LevelCompleted(placeCompleted = true),
            GameEvent.PlaceCompleted(place, 25, "Yunanistan"),
        ).forEach(sink::onEvent)
        // aynı anda: CORRECT + COIN + LEVEL_COMPLETE(2.1 sn) + PLACE_COMPLETE(2.05 sn)
        assertFalse("iki uzun jingle aynı anda başlıyor: ${rec.played}", Sfx.LEVEL_COMPLETE in rec.played && Sfx.PLACE_COMPLETE in rec.played)
    }

    @Test fun BUG_simultaneousSfxOnCountryFinishFitInFourStreams() {
        val rec = Recorder()
        val sink = SoundFeedbackSink(rec)
        // son harf sesi hâlâ çalarken (0.35 sn) ülke biter: 1 harf + 4 efekt = 5 > MAX_STREAMS(4)
        listOf(
            GameEvent.LetterAdded('t', 0, 3), GameEvent.Submitted(SubmitResult.Correct("net")), GameEvent.CoinsEarned(10, silent = true),
            GameEvent.LevelCompleted(placeCompleted = true), GameEvent.PlaceCompleted(place, 25, null),
        ).forEach(sink::onEvent)
        assertTrue("eşzamanlı ${rec.played.size} > ${com.kelimeoyunu.audio.SoundManager.MAX_STREAMS}: ${rec.played}", rec.played.size <= com.kelimeoyunu.audio.SoundManager.MAX_STREAMS)
    }

    @Test fun BUG_spokenCountryIntroStartsOnlyAfterTheRegionMelodyHasEnded() {
        val delayAndBatch = PLACE_INTRO_DELAY_MS + Announcer.DEFAULT_BATCH_MS
        for (r in listOf("avrupa", "asya", "ortadogu", "afrika", "amerika", "okyanusya")) {
            val f = TestAssets.file("sounds/place_$r.wav")
            val seconds = (f.length() - 44) / 2.0 / 44100.0
            assertTrue("place_$r.wav ${"%.2f".format(seconds)} sn, konuşma ${delayAndBatch} ms sonra başlıyor (çakışma)", seconds * 1000 + 50 <= delayAndBatch)
        }
    }

    @Test fun everyRegionHasItsOwnMelodyAndNothingFallsBackToNav() {
        val sfx = listOf("avrupa", "asya", "ortadogu", "afrika", "amerika", "okyanusya").map { r ->
            sfxFor(GameEvent.PlaceEntered(place.copy(bolge = r), 20)).single()
        }
        assertEquals(6, sfx.toSet().size)
        assertFalse(Sfx.NAV in sfx)
        assertEquals(listOf(Sfx.PLACE_COMPLETE), sfxFor(GameEvent.PlaceCompleted(place, 25, null)))
    }

    @Test fun compositeSinkDoesNotReorderSpeechAndSound() {
        val order = ArrayList<String>()
        val a = com.kelimeoyunu.ui.game.FeedbackSink { order += "a:$it" }
        val b = com.kelimeoyunu.ui.game.FeedbackSink { order += "b:$it" }
        CompositeFeedbackSink(a, b).onEvent(GameEvent.LevelCompleted())
        assertEquals(listOf("a:LevelCompleted(placeCompleted=false)", "b:LevelCompleted(placeCompleted=false)"), order)
    }
}
