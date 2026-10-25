package com.kelimeoyunu.ui.game

import com.kelimeoyunu.domain.SubmitResult

/**
 * Çift dokunuşla gelen ikinci gönderim (kelime zaten temizlendiği için `Empty`), anlamlı bir
 * sonuçtan sonraki [graceMs] içinde bastırılır. Konuşma ve ses aynı kuralı paylaşsın diye her
 * [FeedbackSink] kendi örneğini kullanır.
 */
class DoubleSubmitGate(
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
    private val graceMs: Long = 800,
) {
    private var lastRealResultAt = Long.MIN_VALUE

    /** true ise olay bastırılmalıdır. */
    @Synchronized
    fun shouldSuppress(e: GameEvent): Boolean {
        if (e !is GameEvent.Submitted) return false
        val now = clock()
        if (e.result == SubmitResult.Empty) {
            return lastRealResultAt != Long.MIN_VALUE && now - lastRealResultAt < graceMs
        }
        lastRealResultAt = now
        return false
    }
}
