pluginManagement {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Sheeps"
include(":app")
include(":core")
include(":feature_splash")
include(":feature_menu")
include(":feature_game")
include(":feature_leaderboard")
include(":baselineprofile")
