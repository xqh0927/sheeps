plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.sheeps.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // 网络层（Retrofit / OkHttp / kotlinx-serialization 经 lib_network 以 api 透传）
    implementation(project(":lib_network"))
    // 基座（MMKV / Hilt / 基础工具）
    implementation(project(":lib_base"))
    // 设计系统（ErrorMessageMapper 引用的 ui.R 字符串资源）
    implementation(project(":designsystem"))

    // Room 本地仓储
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt DI（Repository @Inject 构造）
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // 序列化（与 lib_network 保持一致，显式声明）
    implementation(libs.kotlinx.serialization.json)

    // 日志 & 存储（data 层使用）
    implementation(libs.logutils.pengwei)
    implementation(libs.mmkv)
}
