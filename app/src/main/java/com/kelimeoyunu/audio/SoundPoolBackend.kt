package com.kelimeoyunu.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.ConcurrentHashMap

/**
 * Gerçek [SoundBackend]. Ses kullanımı USAGE_GAME: medya ses seviyesine uyar ve kullanıcı kısabilir.
 * USAGE_ASSISTANCE_ACCESSIBILITY seçilmedi: o akış TalkBack'in kendi konuşma/erişilebilirlik
 * seviyesine bağlıdır, oyun efektleri onunla karışıp konuşmayı bastırabilir. SoundPool ses odağı
 * istemez, bu yüzden TalkBack konuşmasını kesmez; TalkBack kendi "sesi kıs" ayarıyla efektleri
 * konuşma sırasında kısabilir (istenen davranış).
 */
class SoundPoolBackend(private val context: Context) : SoundBackend {
    private val pool = SoundPool.Builder()
        .setMaxStreams(SoundManager.MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = ConcurrentHashMap<Sfx, Int>()
    private val callbacks = ConcurrentHashMap<Int, Pair<Sfx, (Boolean) -> Unit>>()

    init {
        pool.setOnLoadCompleteListener { _, soundId, status ->
            callbacks.remove(soundId)?.let { (sfx, cb) ->
                if (status == 0) ids[sfx] = soundId
                cb(status == 0)
            }
        }
    }

    override fun load(sfx: Sfx, onLoaded: (Boolean) -> Unit) {
        try {
            val id = context.assets.openFd("sounds/${sfx.fileName}").use { pool.load(it, 1) }
            if (id == 0) { onLoaded(false); return }
            callbacks[id] = sfx to onLoaded
        } catch (_: java.io.IOException) {
            onLoaded(false)
        }
    }

    override fun play(sfx: Sfx, rate: Float) {
        val id = ids[sfx] ?: return
        pool.play(id, 1f, 1f, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    override fun release() {
        callbacks.clear(); ids.clear()
        pool.release()
    }
}
