package com.kelimeoyunu.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProgressRepositoryTest {
    private lateinit var dir: File
    private lateinit var file: File
    private val scopes = ArrayList<CoroutineScope>()

    @Before
    fun setUp() {
        dir = kotlin.io.path.createTempDirectory("progress").toFile()
        file = File(dir, "progress.preferences_pb")
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        dir.deleteRecursively()
    }

    private fun store(): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { file },
        )
    }

    private val default = Progress(0, 50, emptySet(), emptySet(), emptySet(), 0)

    @Test
    fun emptyStoreGivesDefaults() = runTest {
        assertEquals(default, ProgressRepository(store()).progress.first())
    }

    @Test
    fun saveThenReadBack() = runTest {
        val repo = ProgressRepository(store())
        val p = Progress(7, 123, setOf("ışık", "çiçek"), setOf("ısı"), setOf(0 to 1, 3 to 4), 3)
        repo.save(p)
        assertEquals(p, repo.progress.first())
    }

    @Test
    fun reopenWithNewRepositoryRestores() = runTest {
        val p = Progress(2, 9, setOf("a", "b"), emptySet(), setOf(5 to 6), 4)
        val first = store()
        ProgressRepository(first).save(p)
        scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() } // dosya kilidi bırakılsın
        scopes.clear()
        assertEquals(p, ProgressRepository(store()).progress.first())
    }

    @Test
    fun savingEmptySetsAfterNonEmptyClearsThem() = runTest {
        val repo = ProgressRepository(store())
        repo.save(Progress(1, 5, setOf("x"), setOf("y"), setOf(1 to 1), 2))
        repo.save(Progress(2, 5, emptySet(), emptySet(), emptySet(), 0))
        assertEquals(Progress(2, 5, emptySet(), emptySet(), emptySet(), 0), repo.progress.first())
    }

    @Test
    fun garbageFieldsFallBackToDefaultsWithoutCrash() = runTest {
        val ds = store()
        ds.edit {
            it[stringPreferencesKey(ProgressKeys.REVEALED)] = "x,y;3;;1,2,3"
            it[intPreferencesKey(ProgressKeys.LEVEL_INDEX)] = -4
            it[intPreferencesKey(ProgressKeys.COINS)] = -10
            it[intPreferencesKey(ProgressKeys.BONUS_METER)] = -1
        }
        assertEquals(default, ProgressRepository(ds).progress.first())
    }

    @Test
    fun wrongTypedValuesDoNotCrash() = runTest {
        val ds = store()
        ds.edit {
            it[stringPreferencesKey(ProgressKeys.COINS)] = "çok"
            it[stringPreferencesKey(ProgressKeys.LEVEL_INDEX)] = "bir"
        }
        assertEquals(default, ProgressRepository(ds).progress.first())
    }

    @Test
    fun corruptFileFallsBackToDefaults() = runTest {
        file.writeBytes(ByteArray(64) { (it * 31 + 7).toByte() })
        assertEquals(default, ProgressRepository(store()).progress.first())
    }
}
