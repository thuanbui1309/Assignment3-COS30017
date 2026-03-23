import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.google.services)
}

val localProperties = File(rootProject.projectDir, "local.properties").let { file ->
    Properties().apply { if (file.exists()) load(file.inputStream()) }
}

android {
    namespace = "com.example.assignment3_cos30017"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.assignment3_cos30017"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SEPAY_API_TOKEN", "\"${localProperties.getProperty("sepay.api.token", "")}\"")
        buildConfigField("String", "SEPAY_BANK_ID", "\"${localProperties.getProperty("sepay.bank.id", "")}\"")
        buildConfigField("String", "SEPAY_ACCOUNT_NUMBER", "\"${localProperties.getProperty("sepay.account.number", "")}\"")
        buildConfigField("String", "SEPAY_ACCOUNT_NAME", "\"${localProperties.getProperty("sepay.account.name", "")}\"")

        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = localProperties.getProperty("google.maps.api.key", "")
    }

    flavorDimensions += "imageStrategy"
    productFlavors {
        create("preload") {
            dimension = "imageStrategy"
            buildConfigField("boolean", "IMAGE_PRELOAD_ALL", "true")
        }
        create("lazy") {
            dimension = "imageStrategy"
            buildConfigField("boolean", "IMAGE_PRELOAD_ALL", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Release-like build used by Macrobenchmark.
        // Installs with debug signing (easy on local devices) but is not debuggable.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += listOf("release")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.viewpager2)

    // UI
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Networking (SePay)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)

    // Google Maps & Location
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Image Loading
    implementation(libs.glide)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.espresso.intents)
}
