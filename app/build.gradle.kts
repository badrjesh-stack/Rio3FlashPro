plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.rio3flashpro"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.rio3flashpro"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.5.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
