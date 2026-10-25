package com.kelimeoyunu.ui.game

/** Oyun olaylarının geri bildirimi (duyuru, ileride ses). Birden fazla uygulama [CompositeFeedbackSink] ile birleşir. */
fun interface FeedbackSink {
    fun onEvent(e: GameEvent)
}

class CompositeFeedbackSink(private val sinks: List<FeedbackSink>) : FeedbackSink {
    constructor(vararg sinks: FeedbackSink) : this(sinks.toList())

    override fun onEvent(e: GameEvent) = sinks.forEach { it.onEvent(e) }
}
