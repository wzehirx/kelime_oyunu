package com.kelimeoyunu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kelimeoyunu.ui.AppRoot
import com.kelimeoyunu.ui.theme.KelimeTheme

class MainActivity : ComponentActivity() {
    private val soundManager get() = (application as KelimeApp).container.soundManager

    // SoundPool yalnızca ön plandayken tutulur; durunca serbest bırakılır, başlayınca yeniden yüklenir.
    override fun onStart() {
        super.onStart()
        soundManager.start()
    }

    override fun onStop() {
        soundManager.release()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as KelimeApp).container
        setContent {
            KelimeTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(container, Modifier.safeDrawingPadding())
                }
            }
        }
    }
}
