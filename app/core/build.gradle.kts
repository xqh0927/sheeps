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
        viewBinding = true
        compose = true
        // 启用 BuildConfig，使 NetworkModule 可按 BuildConfig.DEBUG 区分构建类型（AGP 8.0+ 库模块默认关闭）
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

kapt {
    correctErrorTypes = true
}

dependencies {
    // 依赖拆分出的基础层与网络层（通过 api 传递依赖，免除 Feature 功能模块重组 build.gradle 依赖）
    api(project(":lib_base"))
    api(project(":lib_network"))

    // Retrofit / OkHttp 等网络及公共类由于在 lib_network 中 api 声明，此处省略

    // Logutils (彭伟日志库)
    api(libs.logutils.pengwei)

    // WorkManager & Hilt Work
    api(libs.workmanager)
    api(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // App Startup
    api(libs.startup)

    // Lifecycle Process (前后台监听)
    api(libs.lifecycle.process)

    // Room Database 本地仓储
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // MMKV / Hilt / Security Crypto
    api(libs.mmkv)
    api(libs.androidx.security.crypto)
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coil 图片加载与常用工具依赖（由 UI 基础组件消费）
    api(libs.coil)
    api(libs.coil.compose)
    api(libs.gson)
    api(libs.xxpermissions)
}



