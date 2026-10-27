plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kelimeoyunu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kelimeoyunu"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.3"
    }

    // Depodaki paylaşılan debug anahtarı: her derleme/CI aynı imzayı üretir, sürümler birbirinin üstüne kurulur.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Release anahtarı yalnızca ortam değişkenlerinden okunur; depoda sır yoktur.
        // KELIME_KEYSTORE yoksa release imzası tanımlanmaz ve assembleRelease imzasız APK üretir.
        val releaseKeystore = System.getenv("KELIME_KEYSTORE")
        if (!releaseKeystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KELIME_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KELIME_KEY_ALIAS")
                keyPassword = System.getenv("KELIME_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 bilerek kapalı: küçültülmüş çıktı bu ortamda bir cihazda çalıştırılıp doğrulanamıyor.
            // Release APK, test edilen debug yapısıyla aynı kodu taşır (fark: imza ve debuggable=false).
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
