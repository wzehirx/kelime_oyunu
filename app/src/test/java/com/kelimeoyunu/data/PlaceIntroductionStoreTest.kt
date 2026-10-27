package com.kelimeoyunu.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
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
import org.junit.Before
import org.junit.Test

/** Kalıcı "tanıtılan en son ülke" sayacı: varsayılan -1, monoton, bozuk kayıt -1, sıfırlamada silinir. */
class PlaceIntroductionStoreTest {
    private lateinit var dir: File
    private lateinit var scope: CoroutineScope
    private lateinit var store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var repo: ProgressRepository

    @Before fun setUp() {
        dir = kotlin.io.path.createTempDirectory("intro").toFile()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = PreferenceDataStoreFactory.create(scope = scope) { File(dir, "p.preferences_pb") }
        repo = ProgressRepository(store)
    }
    @After fun tearDown() { scope.cancel(); dir.deleteRecursively() }

    @Test fun defaultsToMinusOne() = runTest { assertEquals(-1, repo.introducedPlaceIndex.first()) }

    @Test fun isMonotonic() = runTest {
        repo.markPlaceIntroduced(0)
        repo.markPlaceIntroduced(3)
        repo.markPlaceIntroduced(2)
        assertEquals(3, repo.introducedPlaceIndex.first())
    }

    @Test fun savingProgressDoesNotTouchIt() = runTest {
        repo.markPlaceIntroduced(4)
        repo.save(Progress(levelIndex = 7, coins = 99))
        assertEquals(4, repo.introducedPlaceIndex.first())
    }

    @Test fun wrongTypeOrNegativeFallsBackToMinusOne() = runTest {
        store.edit { it[stringPreferencesKey(ProgressKeys.INTRODUCED_PLACE)] = "abc" }
        assertEquals(-1, repo.introducedPlaceIndex.first())
        repo.markPlaceIntroduced(2) // bozuk kayıt üzerine yazılabilir
        assertEquals(2, repo.introducedPlaceIndex.first())
    }

    @Test fun resetClearsProgressAndIntroduction() = runTest {
        repo.save(Progress(levelIndex = 30, coins = 5, found = setOf("ten")))
        repo.markPlaceIntroduced(2)
        repo.reset()
        assertEquals(Progress(), repo.progress.first())
        assertEquals(-1, repo.introducedPlaceIndex.first())
    }
}
