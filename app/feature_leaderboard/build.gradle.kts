plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    }

android {
    namespace = "com.example.sheeps.leaderboard"
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

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(project(":core"))
    implementation(libs.therouter)
    kapt(libs.therouter.apt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // BRVAH, SmartRefreshLayout, Lottie
    implementation(libs.brvah)
    implementation(libs.smart.refresh.kernel)
    implementation(libs.smart.refresh.header)
    implementation(libs.lottie)
}




