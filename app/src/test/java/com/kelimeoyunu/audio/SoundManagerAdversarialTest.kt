package com.kelimeoyunu.audio

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundManagerAdversarialTest {

    /** İş parçacığı güvenli sahte backend. */
    private class SafeBackend(val autoComplete: Boolean = true) : SoundBackend {
        val plays = AtomicInteger()
        val released = java.util.concurrent.atomic.AtomicBoolean(false)
        val playAfterRelease = AtomicInteger()
        private val pending = ConcurrentLinkedQueue<() -> Unit>()
        override fun load(sfx: Sfx, onLoaded: (Boolean) -> Unit) {
            if (autoComplete) onLoaded(true) else pending += { onLoaded(true) }
        }
        fun completeLoads() { while (true) (pending.poll() ?: break).invoke() }
        override fun play(sfx: Sfx, rate: Float) { if (released.get()) playAfterRelease.incrementAndGet(); plays.incrementAndGet() }
        override fun release() { released.set(true) }
    }

    @Test fun staleLoadCallbacksFromReleasedBackendNeverEnableTheNewOne() {
        val b1 = SafeBackend(autoComplete = false)
        val b2 = SafeBackend(autoComplete = false)
        val q = ArrayDeque(listOf(b1, b2))
        val m = SoundManager { q.removeFirst() }
        m.enabled = true
        m.start(); m.release(); m.start()
        b1.completeLoads() // eski nesil: yeni backend'i "yüklendi" saymamalı
        m.play(Sfx.LETTER)
        assertEquals(0, b1.plays.get()); assertEquals(0, b2.plays.get())
        b2.completeLoads()
        m.play(Sfx.LETTER)
        assertEquals(1, b2.plays.get())
    }

    @Test fun playAfterReleaseAndBeforeStartIsSilent() {
        val b = SafeBackend()
        val m = SoundManager { b }
        m.enabled = true
        m.play(Sfx.CORRECT)               // hiç başlatılmadı
        m.start(); m.release()
        m.play(Sfx.CORRECT)               // bırakıldı
        assertEquals(0, b.plays.get())
    }

    @Test fun disablingTakesEffectImmediatelyAndEnablingResumes() {
        val b = SafeBackend()
        val m = SoundManager { b }
        m.start(); m.enabled = true
        m.play(Sfx.LETTER); m.enabled = false; m.play(Sfx.LETTER); m.play(Sfx.CORRECT)
        m.enabled = true; m.play(Sfx.LETTER)
        assertEquals(2, b.plays.get())
    }

    @Test fun enabledDefaultsToFalseUntilSettingIsRead() {
        val b = SafeBackend()
        val m = SoundManager { b }
        m.start()
        m.play(Sfx.LETTER)
        assertEquals(0, b.plays.get())
    }

    @Test fun factoryFailureThenRecovery() {
        var calls = 0
        val good = SafeBackend()
        val m = SoundManager { if (calls++ == 0) error("SoundPool oluşturulamadı") else good }
        m.enabled = true
        m.start(); m.play(Sfx.LETTER)
        assertEquals(0, good.plays.get())
        m.start(); m.play(Sfx.LETTER)  // ikinci start yeniden denemeli
        assertEquals(1, good.plays.get())
    }

    @Test fun backendThatThrowsOnPlayOrReleaseNeverEscapes() {
        val bad = object : SoundBackend {
            override fun load(sfx: Sfx, onLoaded: (Boolean) -> Unit) = onLoaded(true)
            override fun play(sfx: Sfx, rate: Float) = error("play")
            override fun release() = error("release")
        }
        val good = SafeBackend()
        val q = ArrayDeque(listOf<SoundBackend>(bad, good))
        val m = SoundManager { q.removeFirst() }
        m.enabled = true
        m.start(); m.play(Sfx.LETTER, 1.2f); m.release()
        m.start(); m.play(Sfx.LETTER)
        assertEquals(1, good.plays.get())
    }

    @Test fun loadCallbackFiringSynchronouslyInsideLoadIsHandled() {
        val b = SafeBackend(autoComplete = true) // load içinde eşzamanlı callback
        val m = SoundManager { b }
        m.enabled = true
        m.start()
        Sfx.entries.forEach { m.play(it) }
        assertEquals(Sfx.entries.size, b.plays.get())
    }

    @Test fun hammerStartReleasePlayAndToggleFromManyThreads() {
        val backends = ConcurrentLinkedQueue<SafeBackend>()
        val m = SoundManager { SafeBackend().also { backends += it } }
        val threads = 8
        val failures = ConcurrentLinkedQueue<Throwable>()
        val done = CountDownLatch(threads)
        repeat(threads) { t ->
            Thread {
                val rnd = Random(t)
                try {
                    repeat(20_000) {
                        when (rnd.nextInt(10)) {
                            0 -> m.start()
                            1 -> m.release()
                            2 -> m.enabled = rnd.nextBoolean()
                            else -> m.play(Sfx.entries[rnd.nextInt(Sfx.entries.size)], 0.8f + rnd.nextFloat())
                        }
                    }
                } catch (e: Throwable) { failures += e }
                done.countDown()
            }.start()
        }
        assertTrue("kilitlenme?", done.await(60, TimeUnit.SECONDS))
        assertTrue("istisna: ${failures.firstOrNull()}", failures.isEmpty())
        // Sonunda temiz bir döngü hâlâ çalışmalı.
        m.release(); m.start(); m.enabled = true
        val last = backends.last()
        val before = last.plays.get()
        m.play(Sfx.CORRECT)
        assertEquals(before + 1, last.plays.get())
        // Her start en fazla bir backend üretir; eşzamanlı start'lar sızıntı yapmamalı.
        val unreleased = backends.count { !it.released.get() }
        assertEquals("sızan (bırakılmamış) backend sayısı", 1, unreleased)
    }
}
