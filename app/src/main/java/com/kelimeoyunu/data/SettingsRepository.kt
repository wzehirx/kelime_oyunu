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

/** Kullanıcı ayarları. Ses efektleri varsayılan olarak açıktır; bozuk değer varsayılana düşer. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val soundKey = booleanPreferencesKey("sound_enabled")

    val soundEnabled: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p -> try { p[soundKey] ?: true } catch (_: ClassCastException) { true } }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[soundKey] = enabled }
    }
}
