package com.kelimeoyunu.ui.game

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.kelimeoyunu.audio.FakeSoundBackend
import com.kelimeoyunu.audio.Sfx
import com.kelimeoyunu.audio.SoundManager
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.domain.WordDictionary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** QA: seçim sırasına bağlı harf sesi, gerçek GameViewModel + SoundFeedbackSink ile uçtan uca. */
@OptIn(ExperimentalCoroutinesApi::class)
class LetterSoundAdversarialTest {

    private class Store : DataStore<Preferences> {
        val flow = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = flow
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val n = transform(flow.value); flow.value = n; return n
        }
    }

    private fun gz(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }

    // 7 harfli seviyeler (biri tekrarlı harf içerir) ve hepsini bitirmesi zor iki kelime.
    private val text = "1|abcdefg|abc,0,0,y;cde,0,2,d\n2|gfedcba|gfe,0,0,y;edc,0,2,d\n3|aabbccd|aab,0,0,y;bcc,0,2,d\n" +
        "4|abcdefg|abc,0,0,y;cde,0,2,d\n5|gfedcba|gfe,0,0,y;edc,0,2,d\n"
    private val dict = WordDictionary(setOf("abc", "cde", "gfe", "edc", "aab", "bcc", "ab", "ba", "dc", "fg", "aa", "bb"))

    private class H(
        val vm: GameViewModel, val events: MutableList<GameEvent>,
        val backend: FakeSoundBackend, val manager: SoundManager,
    )

    private fun TestScope.start(seed: Int, soundOn: Boolean = true): H {
        val d = StandardTestDispatcher(testScheduler)
        val vm = GameViewModel(
            LevelRepository { ByteArrayInputStream(gz(text)) }, { dict }, ProgressRepository(Store()), Random(seed), d,
        )
        val backend = FakeSoundBackend()
        val manager = SoundManager({ backend }).also { it.start(); it.enabled = soundOn }
        val sink = SoundFeedbackSink(manager)
        val events = ArrayList<GameEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events += it; sink.onEvent(it) }
        }
        runCurrent()
        return H(vm, events, backend, manager)
    }

    private fun GameViewModel.ready() = state.value as GameUiState.Ready
    private val letterSfx = setOf(Sfx.LETTER_1, Sfx.LETTER_2, Sfx.LETTER_3, Sfx.LETTER_4, Sfx.LETTER_5, Sfx.LETTER_6, Sfx.LETTER_7)

    @Test fun randomActionsKeepPositionEqualToCurrentWordLength() = runTest {
        for (seed in 1..8) {
            val h = start(seed)
            val r = Random(seed * 7919)
            var letterEvents = 0
            repeat(200) { step ->
                val s0 = h.vm.state.value
                if (s0 !is GameUiState.Ready) return@repeat
                val before = h.events.size
                val lenBefore = s0.currentWord.length
                when (r.nextInt(10)) {
                    0, 1, 2, 3 -> h.vm.onLetter(r.nextInt(-1, 8))
                    4 -> h.vm.onDelete()
                    5 -> h.vm.onClear()
                    6 -> h.vm.onShuffle()
                    7 -> h.vm.onSubmit()
                    8 -> h.vm.onNextLevel()
                    else -> h.vm.onHintLetter()
                }
                val s1 = h.vm.state.value
                val added = h.events.drop(before).filterIsInstance<GameEvent.LetterAdded>()
                if (s1 is GameUiState.Ready && s0.levelNumber == s1.levelNumber) {
                    if (s1.currentWord.length == lenBefore + 1) {
                        assertEquals("seed=$seed step=$step", 1, added.size)
                        // position, olay anındaki currentWord uzunluğu
                        assertEquals("seed=$seed step=$step", s1.currentWord.length, added[0].position)
                        assertTrue(added[0].position in 1..s1.tiles.size)
                        assertEquals(s1.currentWord.last(), added[0].char)
                    } else {
                        // yok sayılan dokunuş (kullanılmış/geçersiz indeks) ses/olay üretmemeli
                        assertTrue("seed=$seed step=$step beklenmeyen LetterAdded", added.isEmpty())
                    }
                }
                letterEvents += added.size
            }
            // Ses dizisi: tam olarak LetterAdded.position sırasıyla LETTER_n
            val expected = h.events.filterIsInstance<GameEvent.LetterAdded>().map { letterSfx.toList()[it.position - 1] }
            assertEquals("seed=$seed", expected, h.backend.played.map { it.first }.filter { it in letterSfx })
            assertEquals(letterEvents, expected.size)
            assertTrue("seed=$seed az harf olayı: ${expected.size}", expected.size > 15)
            assertTrue(h.backend.played.all { it.second == 1f })
        }
    }

    @Test fun soundOffNeverPlaysAnything() = runTest {
        val h = start(5, soundOn = false)
        val r = Random(42)
        repeat(200) {
            when (r.nextInt(8)) {
                0, 1, 2 -> h.vm.onLetter(r.nextInt(0, 7))
                3 -> h.vm.onDelete(); 4 -> h.vm.onClear(); 5 -> h.vm.onShuffle()
                6 -> h.vm.onSubmit(); else -> h.vm.onHintLetter()
            }
        }
        assertTrue(h.events.isNotEmpty())
        assertTrue(h.backend.played.isEmpty())
    }

    @Test fun sevenLettersPlayAscendingNotesOneToSeven() = runTest {
        val h = start(1)
        assertEquals(7, h.vm.ready().tiles.size)
        h.vm.state.value.let { s -> (s as GameUiState.Ready).tiles.forEach { h.vm.onLetter(it.index) } }
        h.vm.onLetter(0) // zaten seçili: ses yok
        assertEquals(
            listOf(Sfx.LETTER_1, Sfx.LETTER_2, Sfx.LETTER_3, Sfx.LETTER_4, Sfx.LETTER_5, Sfx.LETTER_6, Sfx.LETTER_7),
            h.backend.played.map { it.first },
        )
    }

    @Test fun afterClearAndAfterShuffleSequenceRestartsOrContinues() = runTest {
        val h = start(2)
        h.vm.onLetter(0); h.vm.onLetter(1); h.vm.onLetter(2)
        h.vm.onShuffle()
        h.vm.onLetter(3)            // karıştırma seçimi bozmaz: 4. nota
        h.vm.onClear()
        h.vm.onLetter(3)            // temizle sonrası 1. nota
        h.vm.onLetter(4); h.vm.onDelete(); h.vm.onDelete()
        h.vm.onLetter(5)            // sil sil -> boş; 1. nota
        h.vm.onSubmit()             // geçersiz kelime: seçim korunur
        h.vm.onLetter(6)            // 2. nota
        val letters = h.backend.played.map { it.first }.filter { it in letterSfx }
        assertEquals(
            listOf(1, 2, 3, 4, 1, 2, 1, 2).map { letterSfx.toList()[it - 1] }, letters,
        )
    }
}
