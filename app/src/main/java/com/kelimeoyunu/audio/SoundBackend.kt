package com.kelimeoyunu.audio

/** SoundPool'u saran ince arayüz; saf mantık sahte uygulamayla test edilir. */
interface SoundBackend {
    /** Sesi yükler; bittiğinde [onLoaded] (başarı bayrağı) çağrılır. Dosya yoksa false ya da IOException. */
    fun load(sfx: Sfx, onLoaded: (Boolean) -> Unit)
    fun play(sfx: Sfx, rate: Float)
    fun release()
}
