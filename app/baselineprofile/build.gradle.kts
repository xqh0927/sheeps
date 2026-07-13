plugins {
    // com.android.test 由 Android Gradle Plugin 直接提供（无需版本）
    id("com.android.test")
    // 官方 Baseline Profile 插件：自动生成 generateBaselineProfile 任务
    alias(libs.plugins.androidx.baselineprofile)
    // Kotlin Gradle 插件：提供 kotlin {} / jvmToolchain 扩展（修复脚本编译错误）
    alias(libs.plugins.kotlin.android)
}


android {
    namespace = "com.example.sheeps.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Baseline Profile 生成需要 API 28+ 的设备/模拟器
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 指定被插桩的目标 App 模块（com.android.test 必须声明）
    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // UI Automator：驱动设备完成启动与基本交互
    implementation(libs.androidx.test.uiautomator)
    // Macrobenchmark：提供 MacrobenchmarkRule 与 MacrobenchmarkScope
    implementation(libs.androidx.benchmark.macro)
}
