package com.kelimeoyunu.data

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {
    private lateinit var dir: File
    private val scopes = ArrayList<CoroutineScope>()

    @Before fun setUp() { dir = kotlin.io.path.createTempDirectory("settings").toFile() }

    @After fun tearDown() {
        scopes.forEach { it.cancel() }
        dir.deleteRecursively()
    }

    private fun store() = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it },
        produceFile = { File(dir, "settings.preferences_pb") },
    )

    @Test fun soundIsOnByDefault() = runTest {
        assertTrue(SettingsRepository(store()).soundEnabled.first())
    }

    @Test fun setOffThenOn() = runTest {
        val r = SettingsRepository(store())
        r.setSoundEnabled(false)
        assertFalse(r.soundEnabled.first())
        r.setSoundEnabled(true)
        assertTrue(r.soundEnabled.first())
    }

    @Test fun valueSurvivesNewInstanceOverSameFile() = runTest {
        SettingsRepository(store()).setSoundEnabled(false)
        scopes.forEach { it.cancel() }
        assertFalse(SettingsRepository(store()).soundEnabled.first())
    }

    @Test fun wrongTypeFallsBackToDefault() = runTest {
        val s = store()
        s.edit { it[stringPreferencesKey("sound_enabled")] = "evet" }
        assertTrue(SettingsRepository(s).soundEnabled.first())
    }

    @Test fun vibrationAndAnimationsAreOnByDefault() = runTest {
        val r = SettingsRepository(store())
        assertTrue(r.vibrationEnabled.first())
        assertTrue(r.animationsEnabled.first())
    }

    @Test fun vibrationAndAnimationsToggleIndependentlyOfSound() = runTest {
        val r = SettingsRepository(store())
        r.setVibrationEnabled(false)
        assertFalse(r.vibrationEnabled.first())
        assertTrue(r.animationsEnabled.first())
        assertTrue(r.soundEnabled.first())
        r.setAnimationsEnabled(false)
        assertFalse(r.animationsEnabled.first())
        r.setSoundEnabled(false)
        assertFalse(r.soundEnabled.first())
        assertFalse(r.vibrationEnabled.first())
        r.setVibrationEnabled(true)
        r.setAnimationsEnabled(true)
        assertTrue(r.vibrationEnabled.first())
        assertTrue(r.animationsEnabled.first())
        assertFalse(r.soundEnabled.first())
    }

    @Test fun newSettingsSurviveNewInstanceOverSameFile() = runTest {
        SettingsRepository(store()).apply { setVibrationEnabled(false); setAnimationsEnabled(false) }
        scopes.forEach { it.cancel() }
        val again = SettingsRepository(store())
        assertFalse(again.vibrationEnabled.first())
        assertFalse(again.animationsEnabled.first())
        assertTrue(again.soundEnabled.first())
    }

    @Test fun wrongTypesForNewSettingsFallBackToTrue() = runTest {
        val s = store()
        s.edit {
            it[stringPreferencesKey("vibration_enabled")] = "hayır"
            it[stringPreferencesKey("animations_enabled")] = "hayır"
        }
        val r = SettingsRepository(s)
        assertTrue(r.vibrationEnabled.first())
        assertTrue(r.animationsEnabled.first())
    }

    @Test fun settingsKeysAreStable() = runTest {
        // Kalıcı anahtar adları: değişirse eski kayıtlar sessizce varsayılana döner.
        val s = store()
        val r = SettingsRepository(s)
        r.setVibrationEnabled(false); r.setAnimationsEnabled(false)
        val keys = s.data.first().asMap().keys.map { it.name }.toSet()
        assertEquals(setOf("vibration_enabled", "animations_enabled"), keys)
    }
}
