package com.kelimeoyunu.audio

import java.util.concurrent.ConcurrentHashMap

/**
 * Ses çalma mantığı. [backendFactory] her [start]'ta yeni backend üretir; [release] sonrası tekrar
 * [start] çağrılabilir. Kapalıyken, başlatılmamışken, ses henüz yüklenmemişken ya da dosya
 * eksikken [play] sessizce yok sayılır. Eşzamanlı akış sınırı ([MAX_STREAMS]) SoundPool'a bırakılır.
 */
class SoundManager(private val backendFactory: () -> SoundBackend) : SfxPlayer {
    /** Ayar kapalıysa hiçbir şey çalmaz. Başlangıçta kapalı: ayar okunana kadar ses yok. */
    @Volatile var enabled: Boolean = false

    private val lock = Any()
    private var backend: SoundBackend? = null
    private var generation = 0
    private val loaded: MutableSet<Sfx> = ConcurrentHashMap.newKeySet()

    fun start() {
        val gen: Int
        val b: SoundBackend
        synchronized(lock) {
            if (backend != null) return
            b = try { backendFactory() } catch (_: Exception) { return }
            backend = b
            gen = ++generation
        }
        for (sfx in Sfx.entries) {
            try {
                b.load(sfx) { ok -> if (ok) synchronized(lock) { if (gen == generation && backend === b) loaded += sfx } }
            } catch (_: Exception) { /* dosya eksik/bozuk: bu sesi atla */ }
        }
    }

    fun play(sfx: Sfx) = play(sfx, 1f)

    override fun play(sfx: Sfx, rate: Float) {
        if (!enabled) return
        val b = synchronized(lock) { if (sfx in loaded) backend else null } ?: return
        try { b.play(sfx, rate) } catch (_: Exception) { }
    }

    fun release() {
        val b = synchronized(lock) {
            generation++
            loaded.clear()
            backend.also { backend = null }
        }
        try { b?.release() } catch (_: Exception) { }
    }

    companion object { const val MAX_STREAMS = 4 }
}

fun interface SfxPlayer {
    fun play(sfx: Sfx, rate: Float)
}
