package com.kelimeoyunu.ui.game

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.kelimeoyunu.domain.SubmitResult

/**
 * Yalnızca görsel tepkiler (çark sallanması, "+bonus" süzülmesi) için sayaçlar. Oyun olaylarından beslenir;
 * odak, anlamsal ağaç ya da duyuru zamanlamasıyla ilgisi yoktur. Sayaç artınca ilgili animasyon bir kez çalar.
 */
@Stable
class GameEffects {
    var shakeTick by mutableIntStateOf(0)
        private set
    var bonusTick by mutableIntStateOf(0)
        private set

    fun onEvent(e: GameEvent) {
        if (e !is GameEvent.Submitted) return
        when (e.result) {
            is SubmitResult.Invalid, SubmitResult.Empty -> shakeTick++
            is SubmitResult.Bonus -> bonusTick++
            is SubmitResult.Correct, is SubmitResult.AlreadyFound -> Unit
        }
    }
}
