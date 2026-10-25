package com.kelimeoyunu.audio

class FakeSoundBackend(
    private val missing: Set<Sfx> = emptySet(),
    private val throwing: Set<Sfx> = emptySet(),
    private val autoComplete: Boolean = true,
) : SoundBackend {
    val played = mutableListOf<Pair<Sfx, Float>>()
    var released = false
    private val pending = mutableListOf<() -> Unit>()
    lateinit var onLoaded: (Sfx, Boolean) -> Unit

    override fun load(sfx: Sfx, onLoaded: (Boolean) -> Unit) {
        if (sfx in throwing) throw java.io.IOException("boom $sfx")
        val action = { onLoaded(sfx !in missing) }
        if (autoComplete) action() else pending += action
    }

    fun completeLoads() { pending.forEach { it() }; pending.clear() }

    override fun play(sfx: Sfx, rate: Float) { played += sfx to rate }
    override fun release() { released = true }
}
