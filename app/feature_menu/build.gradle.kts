plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    }


android {
    namespace = "com.example.sheeps.menu"
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
    implementation(project(":lib_base"))
    implementation(project(":lib_network"))
    implementation(libs.therouter)
    implementation(libs.kotlinx.serialization.json)
    // 以下依赖原本由 :core 以 api 透传，收紧后归还给真正使用它们的 feature
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.lifecycle.process)
    kapt(libs.therouter.apt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.luban)
    implementation(libs.cropper)
 }




