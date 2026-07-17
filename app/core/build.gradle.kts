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
    // 依赖拆分出的基础层与网络层（收紧为 implementation，防止依赖透传给 Feature 模块）
    implementation(project(":lib_base"))
    implementation(project(":lib_network"))

    // 物理拆分出的数据层与设计系统（以 api 暴露给 Feature 模块）
    api(project(":data"))
    api(project(":designsystem"))

    // Retrofit / OkHttp 等网络及公共类由于在 lib_network 中 api 声明，此处省略

    // Logutils (彭伟日志库)
    implementation(libs.logutils.pengwei)

    // WorkManager & Hilt Work
    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // App Startup
    implementation(libs.startup)

    // Logcat（core 的 LogcatInitializer 通过 App Startup 安装，原经 lib_base api 透传，收紧后改为 core 直依赖）
    implementation(libs.logcat)

    // Lifecycle Process (前后台监听)
    implementation(libs.lifecycle.process)

    // Room Database 本地仓储
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // MMKV / Hilt / Security Crypto
    implementation(libs.mmkv)
    implementation(libs.androidx.security.crypto)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coil 图片加载依赖（由 UI 基础组件消费）
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.xxpermissions)
}



