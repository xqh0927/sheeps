plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    }


android {
    namespace = "com.example.sheeps.game"
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
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":designsystem"))
    implementation(project(":lib_base"))
    implementation(libs.therouter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logutils.pengwei)
    kapt(libs.therouter.apt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coil 图片加载库 — 支持动画 WebP（灵动动画系列皮肤）
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
}




