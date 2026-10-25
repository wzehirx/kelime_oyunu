package com.kelimeoyunu.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

const val START_COINS = 50

data class Progress(
    val levelIndex: Int = 0,
    val coins: Int = START_COINS,
    val found: Set<String> = emptySet(),
    val bonusFound: Set<String> = emptySet(),
    val revealed: Set<Pair<Int, Int>> = emptySet(),
    val bonusMeter: Int = 0,
)

internal object ProgressKeys {
    const val LEVEL_INDEX = "level_index"
    const val COINS = "coins"
    const val FOUND = "found"
    const val BONUS_FOUND = "bonus_found"
    const val REVEALED = "revealed"
    const val BONUS_METER = "bonus_meter"
}

/** İlerlemeyi Preferences DataStore'da tutar. Bozuk/eksik alanlar çökmeden varsayılana döner. */
class ProgressRepository(private val dataStore: DataStore<Preferences>) {
    val progress: Flow<Progress> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { decode(it) }

    suspend fun save(p: Progress) {
        dataStore.edit {
            it[intKey(ProgressKeys.LEVEL_INDEX)] = p.levelIndex
            it[intKey(ProgressKeys.COINS)] = p.coins
            it[intKey(ProgressKeys.BONUS_METER)] = p.bonusMeter
            it[strKey(ProgressKeys.FOUND)] = p.found.joinToString(",")
            it[strKey(ProgressKeys.BONUS_FOUND)] = p.bonusFound.joinToString(",")
            it[strKey(ProgressKeys.REVEALED)] = p.revealed.joinToString(";") { (r, c) -> "$r,$c" }
        }
    }

    private fun intKey(n: String) = intPreferencesKey(n)
    private fun strKey(n: String) = stringPreferencesKey(n)

    private fun decode(p: Preferences): Progress {
        val d = Progress()
        return Progress(
            levelIndex = int(p, ProgressKeys.LEVEL_INDEX, d.levelIndex),
            coins = int(p, ProgressKeys.COINS, d.coins),
            found = words(p, ProgressKeys.FOUND),
            bonusFound = words(p, ProgressKeys.BONUS_FOUND),
            revealed = cells(p, ProgressKeys.REVEALED),
            bonusMeter = int(p, ProgressKeys.BONUS_METER, d.bonusMeter),
        )
    }

    // Anahtar başka türde kayıtlıysa p[key] ClassCastException atar; varsayılana düşeriz.
    private fun int(p: Preferences, name: String, default: Int): Int =
        try { p[intKey(name)]?.takeIf { it >= 0 } ?: default } catch (_: ClassCastException) { default }

    private fun str(p: Preferences, name: String): String =
        try { p[strKey(name)] ?: "" } catch (_: ClassCastException) { "" }

    private fun words(p: Preferences, name: String): Set<String> =
        str(p, name).split(',').filter { it.isNotBlank() }.toSet()

    private fun cells(p: Preferences, name: String): Set<Pair<Int, Int>> {
        val result = HashSet<Pair<Int, Int>>()
        for (part in str(p, name).split(';')) {
            if (part.isBlank()) continue
            val f = part.split(',')
            val r = f.getOrNull(0)?.toIntOrNull()
            val c = f.getOrNull(1)?.toIntOrNull()
            if (f.size != 2 || r == null || c == null || r < 0 || c < 0) return emptySet() // bozuk: tümünü at
            result += r to c
        }
        return result
    }
}
