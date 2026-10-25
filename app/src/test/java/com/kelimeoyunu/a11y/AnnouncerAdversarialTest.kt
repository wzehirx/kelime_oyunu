package com.kelimeoyunu.a11y

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncerAdversarialTest {

    /** Üretimdeki gibi tek iş parçacıklı (Main benzeri) dağıtıcıda, çok iş parçacığından yoğun yük. */
    @Test fun heavyConcurrentLoadLosesNothingAndKeepsPerProducerOrder() {
        val exec = Executors.newSingleThreadExecutor()
        val scope = CoroutineScope(SupervisorJob() + exec.asCoroutineDispatcher())
        val out = java.util.Collections.synchronizedList(ArrayList<String>())
        val announcer = Announcer({ out += it }, scope, batchMs = 3)
        val threads = 8
        val perThread = 2000
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        repeat(threads) { t ->
            Thread {
                start.await()
                for (i in 0 until perThread) announcer.say("t$t-$i")
                done.countDown()
            }.start()
        }
        start.countDown()
        assertTrue(done.await(60, TimeUnit.SECONDS))
        // Son toplu gönderimin bitmesini bekle.
        val deadline = System.currentTimeMillis() + 10_000
        fun tokens() = synchronized(out) { out.flatMap { it.split(". ") } }
        while (tokens().size < threads * perThread && System.currentTimeMillis() < deadline) Thread.sleep(20)
        val all = tokens()
        assertEquals("kayıp ya da çift duyuru", threads * perThread, all.size)
        assertEquals(threads * perThread, all.toSet().size)
        for (t in 0 until threads) {
            val mine = all.filter { it.startsWith("t$t-") }.map { it.substringAfter("-").toInt() }
            assertEquals("t$t sırası bozuldu", mine.sorted(), mine)
        }
        scope.cancel(); exec.shutdownNow()
    }

    @Test fun interruptibleLettersAreReplacedButResultsSurvive() = runTest {
        val out = ArrayList<String>()
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val a = Announcer({ out += it }, scope, batchMs = 120)
        a.say("A", interrupt = true)
        a.say("B", interrupt = true)
        a.say("Doğru, TEN")
        a.say("C", interrupt = true)
        scope.advanceTimeBy(121)
        scope.testScheduler.runCurrent()
        assertEquals(listOf("Doğru, TEN. C"), out)
    }

    @Test fun separateWindowsAreSeparateMessagesInOrder() = runTest {
        val out = ArrayList<String>()
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val a = Announcer({ out += it }, scope, batchMs = 120)
        a.say("T", interrupt = true)
        scope.advanceTimeBy(121); scope.testScheduler.runCurrent()
        a.say("E", interrupt = true)
        scope.advanceTimeBy(121); scope.testScheduler.runCurrent()
        a.say("Doğru, TE")
        scope.advanceTimeBy(121); scope.testScheduler.runCurrent()
        assertEquals(listOf("T", "E", "Doğru, TE"), out)
    }

    @Test fun throwingSinkDoesNotWedgeLaterAnnouncements() {
        val exec = Executors.newSingleThreadExecutor()
        val swallowed = java.util.concurrent.atomic.AtomicInteger()
        val scope = CoroutineScope(
            SupervisorJob() + exec.asCoroutineDispatcher() + CoroutineExceptionHandler { _, _ -> swallowed.incrementAndGet() },
        )
        val out = java.util.Collections.synchronizedList(ArrayList<String>())
        var first = true
        val a = Announcer({ if (first) { first = false; error("boom") } else out += it }, scope, batchMs = 5)
        a.say("bir")
        Thread.sleep(100)
        a.say("iki")
        val deadline = System.currentTimeMillis() + 5_000
        while (out.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertEquals(listOf("iki"), out.toList())
        scope.cancel(); exec.shutdownNow()
    }

    @Test fun blankAndWhitespaceMessagesAreDropped() = runTest {
        val out = ArrayList<String>()
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val a = Announcer({ out += it }, scope, batchMs = 10)
        a.say(""); a.say("   "); a.say("\n")
        scope.advanceTimeBy(50); scope.testScheduler.runCurrent()
        assertTrue(out.isEmpty())
    }
}
