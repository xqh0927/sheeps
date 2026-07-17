plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.sheeps.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
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
    // 基座（Compose BOM / material3 / therouter / mmkv / hilt 等经 api 透传）
    api(project(":lib_base"))

    // Coil 图片加载（RemoteImage 等组件消费）
    implementation(libs.coil)
    implementation(libs.coil.compose)
}
