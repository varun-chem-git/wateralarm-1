plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.waterbuddy.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.waterbuddy.app"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // A fixed keystore is committed so every build has the SAME signature.
    // That lets a new APK install over the old one without uninstalling first.
    signingConfigs {
        create("release") {
            storeFile = file("water-buddy.jks")
            storePassword = "waterbuddy"
            keyAlias = "waterbuddy"
            keyPassword = "waterbuddy"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
