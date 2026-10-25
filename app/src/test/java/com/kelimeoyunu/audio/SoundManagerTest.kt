package com.kelimeoyunu.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundManagerTest {
    private fun manager(vararg backends: FakeSoundBackend, enabled: Boolean = true): SoundManager {
        val q = backends.toMutableList()
        return SoundManager({ q.removeAt(0) }).also { it.enabled = enabled }
    }

    @Test fun playsLoadedSoundWithRate() {
        val b = FakeSoundBackend(); val m = manager(b)
        m.start(); m.play(Sfx.COIN, 1.2f)
        assertEquals(listOf(Sfx.COIN to 1.2f), b.played)
    }

    @Test fun disabledPlaysNothing() {
        val b = FakeSoundBackend(); val m = manager(b, enabled = false)
        m.start(); Sfx.entries.forEach { m.play(it) }
        assertTrue(b.played.isEmpty())
        m.enabled = true; m.play(Sfx.NAV)
        assertEquals(1, b.played.size)
    }

    @Test fun playBeforeLoadCompletesIsIgnoredWithoutCrash() {
        val b = FakeSoundBackend(autoComplete = false); val m = manager(b)
        m.play(Sfx.LETTER)          // start() yok
        m.start(); m.play(Sfx.LETTER) // yükleme bitmedi
        assertTrue(b.played.isEmpty())
        b.completeLoads(); m.play(Sfx.LETTER)
        assertEquals(1, b.played.size)
    }

    @Test fun missingOrThrowingFilesAreSkippedOthersStillPlay() {
        val b = FakeSoundBackend(missing = setOf(Sfx.CLEAR), throwing = setOf(Sfx.HINT)); val m = manager(b)
        m.start()
        m.play(Sfx.CLEAR); m.play(Sfx.HINT); m.play(Sfx.DELETE)
        assertEquals(listOf(Sfx.DELETE), b.played.map { it.first })
    }

    @Test fun releaseThenStartReloadsOnFreshBackend() {
        val b1 = FakeSoundBackend(); val b2 = FakeSoundBackend(); val m = manager(b1, b2)
        m.start(); m.release()
        assertTrue(b1.released)
        m.play(Sfx.COIN)               // serbest bırakılmış: yok sayılır
        assertTrue(b1.played.isEmpty())
        m.start(); m.play(Sfx.COIN)
        assertEquals(1, b2.played.size)
    }

    @Test fun startIsIdempotentAndReleaseTwiceIsSafe() {
        val b = FakeSoundBackend(); val m = manager(b)
        m.start(); m.start(); m.release(); m.release()
        assertTrue(b.released)
    }

    @Test fun lateLoadCallbackAfterReleaseIsHarmless() {
        val b1 = FakeSoundBackend(autoComplete = false); val b2 = FakeSoundBackend(); val m = manager(b1, b2)
        m.start(); m.release(); m.start()
        b1.completeLoads()             // eski backend'in gecikmiş geri çağrısı
        m.play(Sfx.COIN)
        assertEquals(1, b2.played.size)
        assertTrue(b1.played.isEmpty())
    }

    @Test fun maxStreamsConstantIsFour() = assertEquals(4, SoundManager.MAX_STREAMS)
}
