package com.kelimeoyunu

import android.content.Context
import android.content.res.AssetManager
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kelimeoyunu.data.LevelRepository
import com.kelimeoyunu.data.ProgressRepository
import com.kelimeoyunu.data.loadWords
import com.kelimeoyunu.ui.game.GameViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.GZIPOutputStream
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Uygulama düzeyinde basit bağımlılık sağlayıcı. Ağır yükleme [loadDispatcher] üzerinde yapılır. */
class AppContainer(
    context: Context,
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val app = context.applicationContext

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { app.preferencesDataStoreFile("progress") },
    )

    val progressRepository = ProgressRepository(dataStore)
    val levelRepository = LevelRepository { openLevelsGzip(app.assets) }

    fun gameViewModelFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameViewModel::class.java))
            return GameViewModel(
                levelRepository,
                { loadWords { app.assets.open("words.txt") } },
                progressRepository,
                Random.Default,
                loadDispatcher,
            ) as T
        }
    }
}

/**
 * Seviye dosyasını gzip akışı olarak verir. Android derleme araçları paketlerken `.gz` uzantılı
 * asset'i açıp uzantısını atar (APK'da düz `levels.txt` bulunur); o durumda düz dosya bellekte
 * yeniden sıkıştırılır. Bu yüzden önce `levels.txt.gz`, yoksa `levels.txt` denenir.
 */
internal fun openLevelsGzip(assets: AssetManager): InputStream =
    try {
        assets.open("levels.txt.gz")
    } catch (_: FileNotFoundException) {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { gz -> assets.open("levels.txt").use { it.copyTo(gz) } }
        ByteArrayInputStream(out.toByteArray())
    }
