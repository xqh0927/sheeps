plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.sheeps.lib_base"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
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
    api(libs.androidx.compose.icons.core)
    api(libs.androidx.compose.icons.extended)

    // TheRouter
    api(libs.therouter)

    // MMKV
    api(libs.mmkv)

    // Toaster & Common Utils（logcat 死依赖已删除，日志统一使用 com.apkfuns.logutils.LogUtils）
    api(libs.toaster)
    api(libs.utilcode)

    // Hilt DI
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
