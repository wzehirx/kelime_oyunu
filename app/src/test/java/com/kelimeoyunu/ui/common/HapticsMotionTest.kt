package com.kelimeoyunu.ui.common

import android.content.Context
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.kelimeoyunu.domain.Place
import com.kelimeoyunu.domain.SubmitResult
import com.kelimeoyunu.ui.game.GameEffects
import com.kelimeoyunu.ui.game.GameEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class HapticsMotionTest {
    @get:Rule val rule = createComposeRule()
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val place = Place("tr", "Türkiye", "Asya", "ortadogu", "Ankara", "a", "b")

    private class RecordingView(context: Context) : View(context) {
        val constants = ArrayList<Int>()
        override fun performHapticFeedback(feedbackConstant: Int): Boolean {
            constants += feedbackConstant
            return true
        }
    }

    // ---- olay -> titreşim türü ----

    @Test fun letterIsATick() = assertEquals(Haptic.Tick, hapticFor(GameEvent.LetterAdded('k', 0, 1)))

    @Test fun correctAndBonusConfirmAndLevelCompleteConfirms() {
        assertEquals(Haptic.Confirm, hapticFor(GameEvent.Submitted(SubmitResult.Correct("kal"))))
        assertEquals(Haptic.Confirm, hapticFor(GameEvent.Submitted(SubmitResult.Bonus("kal"))))
        assertEquals(Haptic.Confirm, hapticFor(GameEvent.LevelCompleted()))
        assertEquals(Haptic.Confirm, hapticFor(GameEvent.LevelCompleted(placeCompleted = true)))
    }

    @Test fun invalidDuplicateEmptyNoCoinsAndNoHintReject() {
        listOf(
            GameEvent.Submitted(SubmitResult.Invalid("x")),
            GameEvent.Submitted(SubmitResult.AlreadyFound("x")),
            GameEvent.Submitted(SubmitResult.Empty),
            GameEvent.NotEnoughCoins,
            GameEvent.NoHintAvailable,
        ).forEach { assertEquals(it.toString(), Haptic.Reject, hapticFor(it)) }
    }

    @Test fun otherEventsDoNotVibrate() {
        listOf(
            GameEvent.Deleted, GameEvent.Cleared, GameEvent.Shuffled, GameEvent.HintUsed(10), GameEvent.CoinsEarned(5),
            GameEvent.BonusMeterFull, GameEvent.PlaceEntered(place, 20), GameEvent.PlaceCompleted(place, 25, null),
            GameEvent.AllLevelsDone,
        ).forEach { assertNull(it.toString(), hapticFor(it)) }
    }

    // ---- View.performHapticFeedback ----

    @Test @Config(sdk = [34]) fun modernApiUsesConfirmAndRejectConstants() {
        val v = RecordingView(context)
        val h = ViewHaptics(v) { true }
        h.perform(Haptic.Tick); h.perform(Haptic.Confirm); h.perform(Haptic.Reject)
        assertEquals(
            listOf(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.CONFIRM, HapticFeedbackConstants.REJECT),
            v.constants,
        )
    }

    @Test @Config(sdk = [29]) fun olderApiFallsBackToKeyboardTap_RejectIsTwoLightTaps() {
        val v = RecordingView(context)
        val h = ViewHaptics(v) { true }
        h.perform(Haptic.Confirm)
        assertEquals(listOf(HapticFeedbackConstants.KEYBOARD_TAP), v.constants)
        v.constants.clear()
        h.perform(Haptic.Reject)
        shadowOf(Looper.getMainLooper()).idle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(200, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals(listOf(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.KEYBOARD_TAP), v.constants)
    }

    @Test fun disabledSettingSilencesEverythingImmediately() {
        val v = RecordingView(context)
        var on = true
        val sink = HapticFeedbackSink(ViewHaptics(v) { on })
        sink.onEvent(GameEvent.LetterAdded('k', 0, 1))
        assertEquals(1, v.constants.size)
        on = false
        sink.onEvent(GameEvent.LetterAdded('a', 1, 2))
        sink.onEvent(GameEvent.Submitted(SubmitResult.Correct("kal")))
        sink.onEvent(GameEvent.Submitted(SubmitResult.Invalid("x")))
        assertEquals("kapalıyken titreşim olmamalı", 1, v.constants.size)
    }

    // ---- hareket ----

    @Test fun motionMsIsZeroWhenDisabled() {
        assertEquals(220, Motion(true).ms(220))
        assertEquals(0, Motion(false).ms(220))
    }

    @Test fun systemAnimatorScaleZeroIsDetected() {
        val resolver = context.contentResolver
        assertFalse("varsayılan ölçek 1", animatorScaleIsZero(resolver))
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertTrue(animatorScaleIsZero(resolver))
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.5f)
        assertFalse(animatorScaleIsZero(resolver))
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    @Test fun provideMotionFollowsSettingAndSystemScale() {
        var setting by mutableStateOf(true)
        var seen: Motion? = null
        rule.setContent { ProvideMotion(setting) { seen = LocalMotion.current } }
        rule.waitForIdle()
        assertTrue(seen!!.enabled)
        setting = false
        rule.waitForIdle()
        assertFalse("ayar kapalı => anlık", seen!!.enabled)
        setting = true
        rule.waitForIdle()
        assertTrue(seen!!.enabled)
    }

    @Test fun systemScaleZeroOverridesAnOpenSetting() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            var seen: Motion? = null
            rule.setContent { ProvideMotion(true) { seen = LocalMotion.current } }
            rule.waitForIdle()
            assertFalse("sistem ölçeği 0 => ayar açık olsa da anlık", seen!!.enabled)
        } finally {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
    }

    // ---- görsel tepki sayaçları ----

    @Test fun effectsCountInvalidAndEmptyAsShakesAndBonusAsFloat() {
        val e = GameEffects()
        e.onEvent(GameEvent.Submitted(SubmitResult.Invalid("x")))
        e.onEvent(GameEvent.Submitted(SubmitResult.Empty))
        assertEquals(2, e.shakeTick)
        e.onEvent(GameEvent.Submitted(SubmitResult.Bonus("kal")))
        assertEquals(1, e.bonusTick)
        e.onEvent(GameEvent.Submitted(SubmitResult.Correct("kal")))
        e.onEvent(GameEvent.Submitted(SubmitResult.AlreadyFound("kal")))
        e.onEvent(GameEvent.LetterAdded('k', 0, 1))
        assertEquals(2, e.shakeTick)
        assertEquals(1, e.bonusTick)
    }

    @Test fun placeInitialsUseOneOrTwoWords() {
        assertEquals("TÜ", placeInitials("Türkiye"))
        assertEquals("JA", placeInitials("Japonya"))
        assertEquals("İS", placeInitials("İspanya"))
        assertEquals("AB", placeInitials("Amerika Birleşik Devletleri"))
        assertEquals("DK", placeInitials("Demokratik Kongo Cumhuriyeti"))
        assertEquals("", placeInitials("  "))
        assertEquals("X", placeInitials("x"))
    }
}
