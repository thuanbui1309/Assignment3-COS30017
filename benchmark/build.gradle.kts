plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.assignment3_cos30017.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Allow running on emulator for convenience (numbers won't reflect real devices).
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Match the app's flavors so Gradle can pick the correct variant.
    flavorDimensions += "imageStrategy"
    productFlavors {
        create("preload") {
            dimension = "imageStrategy"
        }
        create("lazy") {
            dimension = "imageStrategy"
        }
    }

    // Match the app's benchmark build type so Macrobenchmark measures a release-like APK.
    buildTypes {
        // com.android.test creates a debug build type by default. Add a benchmark build type
        // so the tested APK can resolve to :app's benchmark variant.
        create("benchmark") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
        }
    }

    // Points this test module at the app-under-test.
    targetProjectPath = ":app"

    // Macrobenchmark runs as a self-instrumenting test APK.
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.junit)
}

