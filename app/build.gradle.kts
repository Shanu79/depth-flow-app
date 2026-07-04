import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.shin.depthflow"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    // 1. Enable BuildConfig generation
    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.shin.depthflow"
        minSdk = 30
        targetSdk = 36
        versionCode = 6
        versionName = "1.05"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 1. Explicitly load local.properties
        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        // 2. Read the ID, clean up any accidental quotes, and inject it
        val rawClientId = properties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""
        val cleanClientId = rawClientId.removePrefix("\"").removeSuffix("\"")

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$cleanClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Core & Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.ui)
    implementation(libs.compose.material3)
    implementation(libs.material)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.navigation.compose)

    // Removed the "1.x.x". The Compose BOM will handle the correct version.
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- Coroutines ---
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.material3)

    // --- Networking (Retrofit, OkHttp, Moshi) ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // --- Images ---
    implementation(libs.coil.compose)

    // --- Database & Code Generation ---
    // Make sure these two lines are in your libs.versions.toml!
    // implementation(libs.androidx.room.runtime)
    // implementation(libs.androidx.room.ktx)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)

    // --- Unit Tests & Roborazzi ---
    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.ktx)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    // --- Android UI Tests ---
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)

    // --- Debugging ---
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Uncomment these when you're ready to use them:
    // implementation(libs.firebase.ai)
    // implementation(libs.play.services.location)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)


    implementation(libs.billing.ktx)

    // In app/build.gradle.kts inside dependencies { ... }
    implementation(libs.moshi.kotlin.v1152)

    // Credential Manager API
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    // Google ID support
    implementation(libs.googleid)

    implementation(libs.androidx.datastore.preferences)

    // Media3 ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.coil.compose.v260)
    implementation(libs.coil.video) // Required for video frames

    implementation(libs.material.icons.extended) // Or your current Compose version

    // For blur effects:
    implementation(libs.haze)
}