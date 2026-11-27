plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val previewStoreFile = providers.gradleProperty("previewStoreFile").orNull ?: System.getenv("ORG_GRADLE_PROJECT_previewStoreFile")
val previewStorePassword = providers.gradleProperty("previewStorePassword").orNull ?: System.getenv("ORG_GRADLE_PROJECT_previewStorePassword")
val previewKeyAlias = providers.gradleProperty("previewKeyAlias").orNull ?: System.getenv("ORG_GRADLE_PROJECT_previewKeyAlias")
val previewKeyPassword = providers.gradleProperty("previewKeyPassword").orNull ?: System.getenv("ORG_GRADLE_PROJECT_previewKeyPassword")
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    signingConfigs {
        create("preview") {
            if (!previewStoreFile.isNullOrBlank()) {
                storeType = "PKCS12"
                storeFile = file(previewStoreFile)
                storePassword = previewStorePassword
                keyAlias = previewKeyAlias
                keyPassword = previewKeyPassword
            }
        }
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
        debug {
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            if (!previewStoreFile.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("preview")
            }
        }
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

dependencies {
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
