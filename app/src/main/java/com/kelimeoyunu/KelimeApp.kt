package com.kelimeoyunu

import android.app.Application

/** Tek bir [AppContainer] örneği tutar (DataStore aynı dosya için çoklu örneğe izin vermez). */
class KelimeApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
