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
}
