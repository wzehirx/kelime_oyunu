package com.kelimeoyunu.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Kullanıcı ayarları. Ses efektleri, titreşim ve animasyonlar varsayılan olarak açıktır; bozuk değer
 * varsayılana düşer.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val soundKey = booleanPreferencesKey("sound_enabled")
    private val vibrationKey = booleanPreferencesKey("vibration_enabled")
    private val animationsKey = booleanPreferencesKey("animations_enabled")

    private fun flagFlow(key: Preferences.Key<Boolean>): Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p -> try { p[key] ?: true } catch (_: ClassCastException) { true } }

    val soundEnabled: Flow<Boolean> = flagFlow(soundKey)
    val vibrationEnabled: Flow<Boolean> = flagFlow(vibrationKey)
    val animationsEnabled: Flow<Boolean> = flagFlow(animationsKey)

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[soundKey] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[vibrationKey] = enabled }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[animationsKey] = enabled }
    }
}
