plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    }

android {
    namespace = "com.example.sheeps.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

kapt {
    correctErrorTypes = true
}

dependencies {
    // Android Core & Lifecycle
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.activity.compose)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    api(composeBom)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    api("androidx.compose.material:material-icons-core")
    api("androidx.compose.material:material-icons-extended")

    // TheRouter
    api(libs.therouter)
    kapt(libs.therouter.apt)

    // Standard Android UI Components (transitive for feature modules)
    api("androidx.recyclerview:recyclerview:1.3.2")
    api("androidx.constraintlayout:constraintlayout:2.1.4")
    api("androidx.appcompat:appcompat:1.6.1")

    // MMKV, Gson, UtilCode, Coil, Logcat, Toaster
    api(libs.mmkv)
    api(libs.gson)
    api(libs.utilcode)
    api(libs.logcat)
    api(libs.toaster)
    api(libs.coil)
    api(libs.coil.compose)

    // Retrofit & Serialization
    api(libs.retrofit)
    api(libs.retrofit.converter.serialization)
    api(libs.kotlinx.serialization.json)
    api(libs.xxpermissions)

    // Hilt DI
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // WorkManager
    api(libs.workmanager)
    api(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // App Startup
    api(libs.startup)

    // Lifecycle Process (foreground/background awareness)
    api(libs.lifecycle.process)

    // Room Database
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Security Crypto Preferences
    api(libs.androidx.security.crypto)
}



