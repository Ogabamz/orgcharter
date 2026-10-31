plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.quiet.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quiet.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
    }

    // A fixed key so every new build installs over the old one without
    // losing your chosen apps. Personal sideload use only.
    signingConfigs {
        create("sideload") {
            storeFile = rootProject.file("keystore/sideload.jks")
            storePassword = "quietlauncher"
            keyAlias = "quiet"
            keyPassword = "quietlauncher"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("sideload")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
