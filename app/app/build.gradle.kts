plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("therouter")
}

android {
    namespace = "com.example.sheeps"
    compileSdk = 36
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val fileName = "sheeps_${variant.versionName}.apk"
                output.outputFileName = fileName
            }
    }
    signingConfigs {
        create("release") {
            // 默认寻址路径相对于当前 app 模块目录
            storeFile = file("../sheeps.jks")
            storePassword = "sheeps123"
            keyAlias = "sheeps"
            keyPassword = "sheeps123"
        }
    }
    defaultConfig {
        applicationId = "com.example.sheeps"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.3"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    // Modular features dependencies
    implementation(project(":core"))
    implementation(project(":feature_splash"))
    implementation(project(":feature_menu"))
    implementation(project(":feature_game"))
    implementation(project(":feature_leaderboard"))

    // TheRouter Annotation Compiler for shell module
    kapt(libs.therouter.apt)
    ksp(libs.hilt.compiler)

    // Hilt must be declared directly in app module (not just via :core api)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)

    // Compose Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}