package com.kelimeoyunu.a11y

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Duyuruları [batchMs] penceresinde toplar ve tek cümlede ". " ile birleştirip [sink]'e verir
 * (örn. "Doğru, KALEM. 10 jeton kazandın. Seviye 3 tamamlandı"). Pencere ilk duyuruyla açılır.
 *
 * `say(text, interrupt = true)` (harf seçimi): penceredeki bekleyen, daha önce `interrupt = true` ile
 * gelmiş eski mesajları atar (yalnızca son harf okunur); diğer duyurular (sonuç, jeton...) korunur.
 */
class Announcer(
    private val sink: (String) -> Unit,
    private val scope: CoroutineScope,
    private val batchMs: Long = DEFAULT_BATCH_MS,
) {
    companion object {
        const val DEFAULT_BATCH_MS = 120L
    }

    private class Item(val text: String, val interruptible: Boolean)

    private val lock = Any()
    private val pending = ArrayList<Item>()
    private var flushJob: Job? = null

    /** [delayMs] > 0: duyuru o kadar sonra, normal toplu akışla verilir (örn. önce melodi çalsın). */
    fun say(text: String, interrupt: Boolean = false, delayMs: Long = 0) {
        if (text.isBlank()) return
        if (delayMs > 0) {
            scope.launch { delay(delayMs); say(text, interrupt) }
            return
        }
        synchronized(lock) {
            if (interrupt) pending.removeAll { it.interruptible }
            pending += Item(text, interrupt)
            if (flushJob == null) flushJob = scope.launch { delay(batchMs); flush() }
        }
    }

    private fun flush() {
        val message = synchronized(lock) {
            flushJob = null
            val m = pending.joinToString(". ") { it.text.trimEnd('.') }
            pending.clear()
            m
        }
        if (message.isNotEmpty()) sink(message)
    }
}
