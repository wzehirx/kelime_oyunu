package com.kelimeoyunu.a11y

import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.GameEvent
import com.kelimeoyunu.ui.game.GameUiState
import com.kelimeoyunu.ui.game.LetterTile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncingFeedbackSinkTest {
    private val spoken = ArrayList<String>()
    private var now = 0L
    private var state: GameUiState = ready()

    private fun ready(word: String = "", coins: Int = 40) = GameUiState.Ready(
        levelNumber = 3,
        tiles = "kalem".mapIndexed { i, c -> LetterTile(i, c, false) },
        currentWord = word, coins = coins, remainingWords = 2, bonusMeter = 0, bonusTarget = 5,
        wordRows = emptyList(), levelComplete = false,
    )

    private fun TestScope.sink() = AnnouncingFeedbackSink(
        Announcer({ spoken += it }, backgroundScope), { state }, { now },
    )

    private suspend fun TestScope.flush() { advanceTimeBy(130); runCurrent() }

    @Test fun correctCoinsAndLevelCompleteAreOneSentence() = runTest {
        val s = sink()
        state = ready()
        s.onEvent(GameEvent.Submitted(SubmitResult.Correct("kalem")))
        s.onEvent(GameEvent.CoinsEarned(10))
        s.onEvent(GameEvent.LevelCompleted)
        flush()
        assertEquals(listOf("Doğru, KALEM. 10 jeton kazandın. Seviye 3 tamamlandı"), spoken)
    }

    @Test fun emptyRightAfterRealResultIsSuppressed() = runTest {
        val s = sink()
        s.onEvent(GameEvent.Submitted(SubmitResult.Correct("kalem"))); flush()
        now = 799
        s.onEvent(GameEvent.Submitted(SubmitResult.Empty)); flush()
        assertEquals(listOf("Doğru, KALEM"), spoken)
    }

    @Test fun emptyRightAfterEachRealResultKindIsSuppressed() = runTest {
        val s = sink()
        listOf(SubmitResult.Bonus("a"), SubmitResult.AlreadyFound("a"), SubmitResult.Invalid("a")).forEach { r ->
            spoken.clear(); now += 10_000
            s.onEvent(GameEvent.Submitted(r)); flush()
            now += 100
            s.onEvent(GameEvent.Submitted(SubmitResult.Empty)); flush()
            assertEquals(1, spoken.size)
        }
    }

    @Test fun emptyAfterGracePeriodIsAnnounced() = runTest {
        val s = sink()
        s.onEvent(GameEvent.Submitted(SubmitResult.Correct("kalem"))); flush()
        now = 800
        s.onEvent(GameEvent.Submitted(SubmitResult.Empty)); flush()
        assertEquals(listOf("Doğru, KALEM", "Önce harf seç"), spoken)
    }

    @Test fun emptyWithoutPriorResultIsAnnounced() = runTest {
        val s = sink()
        s.onEvent(GameEvent.Submitted(SubmitResult.Empty)); flush()
        assertEquals(listOf("Önce harf seç"), spoken)
    }

    @Test fun deleteShuffleHintUseCurrentState() = runTest {
        val s = sink()
        state = ready(word = "ka")
        s.onEvent(GameEvent.Deleted); flush()
        s.onEvent(GameEvent.Shuffled); flush()
        s.onEvent(GameEvent.HintUsed); flush()
        assertEquals(
            listOf("Silindi, kalan: KA", "Harfler karıştırıldı: K, A, L, E, M", "İpucu kullanıldı, kalan jeton: 40"),
            spoken,
        )
    }

    @Test fun remainingEventsMapToTheirTexts() = runTest {
        val s = sink()
        listOf(
            GameEvent.Cleared, GameEvent.NotEnoughCoins, GameEvent.NoHintAvailable,
            GameEvent.BonusMeterFull, GameEvent.AllLevelsDone,
        ).forEach { s.onEvent(it); flush() }
        assertEquals(
            listOf("Temizlendi", "Yeterli jeton yok", "Açılacak ipucu yok", "Bonus sayacı doldu", "Tebrikler, tüm seviyeleri tamamladın"),
            spoken,
        )
    }

    @Test fun rapidLettersThenResultKeepResult() = runTest {
        val s = sink()
        "kalem".forEachIndexed { i, c -> s.onEvent(GameEvent.LetterAdded(c, i)); advanceTimeBy(10) }
        s.onEvent(GameEvent.Submitted(SubmitResult.Correct("kalem")))
        flush()
        assertEquals(listOf("M. Doğru, KALEM"), spoken)
    }
}
