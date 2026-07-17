plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
    id("therouter")
}

android {
    namespace = "com.example.sheeps"
    compileSdk = 37
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
        // 仅保留应用实际提供的语言资源，缩减 APK 体积（values 默认目录始终保留）。
        // 实际语言目录：values(默认), values-en, values-ja, values-ko, values-zh-rTW
        resConfigs("en", "ja", "ko", "zh-rTW")
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.8"
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
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    // Baseline Profile：关联 baselineprofile 测试模块。
    // 执行 `./gradlew :baselineprofile:generateBaselineProfile`（需连接设备/模拟器）后，
    // 生成的 profile 会在 `./gradlew :app:assembleRelease` 时自动合并进 release APK，
    // 提升启动与关键场景的运行速度。
    baselineProfile {
        from(project(":baselineprofile"))
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
    implementation(project(":lib_base"))
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
    implementation(libs.workmanager)
    implementation(libs.lifecycle.process)

    // Compose Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}