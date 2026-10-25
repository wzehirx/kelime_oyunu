package com.kelimeoyunu

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Derleme yapılandırması sözleşmeleri (APK içeriği ve imza). */
class BuildConfigAdversarialTest {
    private fun gradle(): String =
        listOf(File("build.gradle.kts"), File("app/build.gradle.kts")).first { it.isFile }.readText()

    /**
     * Depoda paylaşılan debug anahtarı (app/debug.keystore) var ve sürümler "debug imzalı APK" olarak
     * yayınlanıyor. Gradle bu anahtarı kullanmazsa CI her seferinde rastgele bir debug anahtarı üretir;
     * kullanıcı v0.3 üstüne v0.4 kuramaz (imza çakışması), kaldırınca ilerleme de gider.
     */
    @Test fun debugBuildIsSignedWithTheCommittedSharedKeystore() {
        val keystore = listOf(File("debug.keystore"), File("app/debug.keystore")).first { it.isFile }
        assertTrue(keystore.length() > 0)
        val text = gradle()
        assertTrue(
            "build.gradle.kts debug.keystore'u signingConfig olarak kullanmıyor",
            text.contains("signingConfigs") && text.contains("debug.keystore"),
        )
    }

    @Test fun manifestDoesNotRequestInternetOrOtherDangerousPermissions() {
        val manifest = listOf(File("src/main/AndroidManifest.xml"), File("app/src/main/AndroidManifest.xml"))
            .first { it.isFile }.readText()
        assertTrue(!manifest.contains("INTERNET"))
        assertTrue(!manifest.contains("uses-permission"))
    }

    @Test fun appDeclaresALauncherIcon() {
        val manifest = listOf(File("src/main/AndroidManifest.xml"), File("app/src/main/AndroidManifest.xml"))
            .first { it.isFile }.readText()
        assertTrue("android:icon yok: başlatıcıda varsayılan sistem simgesi görünür", manifest.contains("android:icon"))
    }
}
