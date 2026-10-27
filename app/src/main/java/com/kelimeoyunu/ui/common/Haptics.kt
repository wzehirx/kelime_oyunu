package com.kelimeoyunu.ui.common

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.FeedbackSink
import com.kelimeoyunu.ui.game.GameEvent

/** Dokunsal geri bildirim türleri (tasarım belirtimi, "Haptics"). */
enum class Haptic { Tick, Confirm, Reject }

fun interface HapticPerformer {
    fun perform(kind: Haptic)
}

/**
 * `View.performHapticFeedback` ile titreşim (izin gerekmez). [enabled] her çağrıda okunur: ayar anında etki eder.
 * API 30 altında CONFIRM yerine KEYBOARD_TAP, REJECT yerine iki hafif KEYBOARD_TAP kullanılır.
 */
class ViewHaptics(private val view: View, private val enabled: () -> Boolean) : HapticPerformer {
    private val handler = Handler(Looper.getMainLooper())

    override fun perform(kind: Haptic) {
        if (!enabled()) return
        when (kind) {
            Haptic.Tick -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            Haptic.Confirm -> view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP,
            )
            Haptic.Reject ->
                if (Build.VERSION.SDK_INT >= 30) {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    handler.postDelayed({ if (enabled()) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }, 70)
                }
        }
    }
}

/**
 * Olay -> titreşim: harf seçimi hafif tık; doğru kelime, bonus ve seviye bitişi onay; geçersiz/zaten bulunmuş/boş
 * gönderim, yetersiz jeton ve ipucu yok ret. Diğer olaylar titreşmez.
 */
fun hapticFor(e: GameEvent): Haptic? = when (e) {
    is GameEvent.LetterAdded -> Haptic.Tick
    is GameEvent.Submitted -> when (e.result) {
        is SubmitResult.Correct, is SubmitResult.Bonus -> Haptic.Confirm
        is SubmitResult.AlreadyFound, is SubmitResult.Invalid, SubmitResult.Empty -> Haptic.Reject
    }
    GameEvent.NotEnoughCoins, GameEvent.NoHintAvailable -> Haptic.Reject
    is GameEvent.LevelCompleted -> Haptic.Confirm
    GameEvent.Deleted, GameEvent.Cleared, GameEvent.Shuffled, is GameEvent.HintUsed,
    is GameEvent.CoinsEarned, GameEvent.BonusMeterFull, is GameEvent.PlaceEntered,
    is GameEvent.PlaceCompleted, GameEvent.AllLevelsDone -> null
}

class HapticFeedbackSink(private val performer: HapticPerformer) : FeedbackSink {
    override fun onEvent(e: GameEvent) {
        hapticFor(e)?.let { performer.perform(it) }
    }
}
