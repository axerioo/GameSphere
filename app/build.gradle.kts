import java.util.Properties

configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
        exclude("com.intellij", "annotations")
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Function to load properties from a file
fun getProps(filePath: String): Properties {
    val props = Properties()
    val propsFile = rootProject.file(filePath)
    if (propsFile.exists() && propsFile.isFile) {
        propsFile.inputStream().use {
            props.load(it)
        }
    }
    return props
}

// Load properties from local.properties
val localProperties = getProps("local.properties")

android {
    namespace = "com.axerioo.gamesphere"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.axerioo.gamesphere"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Provide keys as BuildConfig fields
        buildConfigField(
            type = "String",
            name = "CLIENT_ID",
            value = "\"${localProperties.getProperty("CLIENT_ID") ?: "YOUR_CLIENT_ID"}\""
        )
        buildConfigField(
            type = "String",
            name = "TWITCH_ACCESS_TOKEN",
            value = "\"${localProperties.getProperty("TWITCH_ACCESS_TOKEN") ?: "YOUR_TWITCH_ACCESS_TOKEN"}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Compose Material Icons
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Retrofit & Moshi
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.moshi.adapters)
    implementation(libs.moshi.kotlin)
    implementation(libs.logging.interceptor) // For logging API requests

    // Coil (Image Loading)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}