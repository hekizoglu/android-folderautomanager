plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.armutlu.apporganizer.benchmark"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Benchmarklar release-benzeri (R8/minify) hedef APK'ya karşı çalışmalı.
        create("benchmarkRelease") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// Sadece benchmarkRelease build type'ı için varyant üret — debug varyantı gereksiz.
androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enable = variant.buildType == "benchmarkRelease"
    }
}

baselineProfile {
    // Yerel makinede otomatik çalıştırma; APK üretiminden ayrı, elle tetiklenir
    // (bkz. CLAUDE.md görev talimatı Adım 4 — connectedAndroidTest).
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.2.4")
}
