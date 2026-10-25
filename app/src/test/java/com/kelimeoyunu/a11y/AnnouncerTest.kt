package com.kelimeoyunu.a11y

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncerTest {
    private val spoken = ArrayList<String>()

    @Test fun nothingIsSpokenBeforeWindowEnds() = runTest {
        val a = Announcer({ spoken += it }, backgroundScope)
        a.say("Merhaba")
        advanceTimeBy(119); runCurrent()
        assertEquals(emptyList<String>(), spoken)
        advanceTimeBy(1); runCurrent()
        assertEquals(listOf("Merhaba"), spoken)
    }

    @Test fun announcementsInSameWindowAreJoinedInOrder() = runTest {
        val a = Announcer({ spoken += it }, backgroundScope)
        a.say("Doğru, KALEM"); a.say("10 jeton kazandın"); a.say("Seviye 3 tamamlandı")
        advanceTimeBy(120); runCurrent()
        assertEquals(listOf("Doğru, KALEM. 10 jeton kazandın. Seviye 3 tamamlandı"), spoken)
    }

    @Test fun separateWindowsAreSeparateMessages() = runTest {
        val a = Announcer({ spoken += it }, backgroundScope)
        a.say("Bir"); advanceTimeBy(120); runCurrent()
        a.say("İki"); advanceTimeBy(120); runCurrent()
        assertEquals(listOf("Bir", "İki"), spoken)
    }

    @Test fun interruptDropsPendingStaleLetter() = runTest {
        val a = Announcer({ spoken += it }, backgroundScope)
        a.say("K", interrupt = true); a.say("A", interrupt = true)
        advanceTimeBy(120); runCurrent()
        assertEquals(listOf("A"), spoken)
    }

    @Test fun fiveQuickLettersReadLastLetter() = runTest {
        val a = Announcer({ spoken += it }, backgroundScope)
        "KALEM".forEach { a.say(it.toString(), interrupt = true); advanceTimeBy(10) }
        advanceTimeBy(200); runCurrent()
        assertEquals(listOf("M"), spoken)
    }

    @Test fun interruptKeepsOtherPendingAnnouncements() = runTest {
        val a = Announcer({ spoken += it }, backgroundScope)
        a.say("Doğru, KALEM"); a.say("10 jeton kazandın")
        a.say("K", interrupt = true)
        advanceTimeBy(120); runCurrent()
        assertEquals(listOf("Doğru, KALEM. 10 jeton kazandın. K"), spoken)
    }

    @Test fun lettersThenResultKeepsBoth() = runTest {
        val a = Announcer({ spoken += it }, backgroundScope)
        a.say("E", interrupt = true); a.say("M", interrupt = true); a.say("Doğru, KALEM")
        advanceTimeBy(120); runCurrent()
        assertEquals(listOf("M. Doğru, KALEM"), spoken)
    }

    @Test fun blankIsIgnoredAndNoTrailingDoubleDots() = runTest {
        val a = Announcer({ spoken += it }, backgroundScope)
        a.say(" "); a.say("Bir."); a.say("İki")
        advanceTimeBy(120); runCurrent()
        assertEquals(listOf("Bir. İki"), spoken)
    }
}
