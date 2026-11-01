plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.hatchzip.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.hatchzip.android"
        minSdk = 33
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        create("release") {
            val customPath = System.getenv("KEYSTORE_PATH")
            val defaultKeystore = file("release-keystore.jks")
            if (!customPath.isNullOrEmpty() && file(customPath).exists()) {
                storeFile = file(customPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "hatchzip123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "hatchzip"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "hatchzip123"
            } else if (defaultKeystore.exists()) {
                storeFile = defaultKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "hatchzip123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "hatchzip"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "hatchzip123"
            } else {
                val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
                if (debugKeystore.exists()) {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/versions/9/previous-compilation-data.bin",
                "DebugProbesKt.bin"
            )
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation("net.java.dev.jna:jna:5.15.0@aar")
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
