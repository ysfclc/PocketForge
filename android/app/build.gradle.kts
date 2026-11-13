plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val releaseStoreFile = providers.gradleProperty("releaseStoreFile").orNull ?: System.getenv("ORG_GRADLE_PROJECT_releaseStoreFile")
val releaseStorePassword = providers.gradleProperty("releaseStorePassword").orNull ?: System.getenv("ORG_GRADLE_PROJECT_releaseStorePassword")
val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias").orNull ?: System.getenv("ORG_GRADLE_PROJECT_releaseKeyAlias")
val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword").orNull ?: System.getenv("ORG_GRADLE_PROJECT_releaseKeyPassword")
android {
    namespace = "com.pocketforge.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pocketforge.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-keystone"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    signingConfigs {
        create("release") {
            if (!releaseStoreFile.isNullOrBlank()) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
