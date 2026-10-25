package com.kelimeoyunu.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Gerçek zamanlı (runBlocking) testler: runTest'in sanal zamanı gerçek IO beklemelerini atlar.
 * Amaç: [ProgressRepository.progress] akışının ardışık kayıtları kaçırmadan yayınladığını kanıtlamak.
 */
class ProgressRepositoryAdversarialTest {
    private lateinit var dir: File
    private val scopes = ArrayList<CoroutineScope>()

    @Before
    fun setUp() {
        dir = kotlin.io.path.createTempDirectory("progress-adv").toFile()
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
            produceFile = { File(dir, "p.preferences_pb") },
        )
    }

    @Test
    fun activeCollectorSeesEverySequentialSaveEventually() = runBlocking {
        val repo = ProgressRepository(store())
        val got = Channel<Progress>(Channel.UNLIMITED)
        val job = CoroutineScope(Dispatchers.Default).launch { repo.progress.collect { got.send(it) } }
        try {
            withTimeout(10_000) { assertEquals(Progress(), got.receive()) }
            var last = -1
            for (i in 1..150) {
                repo.save(Progress(i, i * 2, setOf("w$i"), emptySet(), setOf(i to i), i % 5))
                withTimeout(10_000) {
                    while (true) {
                        val p = got.receive()
                        assertTrue("geriye gitti: ${p.levelIndex} < $last", p.levelIndex >= last)
                        last = p.levelIndex
                        if (p.levelIndex >= i) {
                            // Gözlenen anlık görüntü tutarlı olmalı (yırtık yazma yok).
                            assertEquals(p.levelIndex * 2, p.coins)
                            assertEquals(setOf("w${p.levelIndex}"), p.found)
                            break
                        }
                    }
                }
            }
            assertEquals(150, last)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun collectorStartedAfterSaveGetsLatestImmediately() = runBlocking {
        val repo = ProgressRepository(store())
        repo.save(Progress(3, 4, emptySet(), emptySet(), emptySet(), 1))
        val p = withTimeout(10_000) { repo.progress.first() }
        assertEquals(Progress(3, 4, emptySet(), emptySet(), emptySet(), 1), p)
    }

    @Test
    fun burstOfUnawaitedSavesEndsWithLastValue() = runBlocking {
        val repo = ProgressRepository(store())
        val jobs = (1..100).map { i ->
            CoroutineScope(Dispatchers.Default).launch { repo.save(Progress(levelIndex = i)) }
        }
        jobs.forEach { it.join() }
        val p = withTimeout(10_000) { repo.progress.first() }
        assertTrue(p.levelIndex in 1..100)
        assertEquals(Progress(levelIndex = p.levelIndex), p)
    }

    @Test
    fun duplicatesAndBlankEntriesAreCleaned() = runBlocking {
        val ds = store()
        ds.edit {
            it[stringPreferencesKey(ProgressKeys.FOUND)] = ",,ışık,, ,ışık,"
            it[stringPreferencesKey(ProgressKeys.REVEALED)] = "1,2;1,2;;3,4"
        }
        val p = ProgressRepository(ds).progress.first()
        assertEquals(setOf("ışık"), p.found)
        assertEquals(setOf(1 to 2, 3 to 4), p.revealed)
    }

    @Test
    fun largeValuesRoundTrip() = runBlocking {
        val repo = ProgressRepository(store())
        val words = (1..5000).map { "kelime$it" }.toSet()
        val cells = (0..400).flatMap { r -> (0..10).map { c -> r to c } }.toSet()
        val p = Progress(9_999, Int.MAX_VALUE, words, words.take(10).toSet(), cells, 4)
        repo.save(p)
        assertEquals(p, repo.progress.first())
    }

    @Test
    fun partiallyBadRevealedDropsOnlyRevealedNotOtherFields() = runBlocking {
        val repo = ProgressRepository(store())
        repo.save(Progress(5, 77, setOf("a"), setOf("b"), emptySet(), 2))
        val ds = store2(repo)
        ds.edit { it[stringPreferencesKey(ProgressKeys.REVEALED)] = "1,2;oops" }
        val p = repo.progress.first()
        assertEquals(Progress(5, 77, setOf("a"), setOf("b"), emptySet(), 2), p)
    }

    // Aynı depoya erişmek için: repo içindeki store'u yeniden üretmeden, saveye yakın bir yol.
    private fun store2(repo: ProgressRepository): DataStore<Preferences> {
        val f = ProgressRepository::class.java.getDeclaredField("dataStore")
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return f.get(repo) as DataStore<Preferences>
    }
}
