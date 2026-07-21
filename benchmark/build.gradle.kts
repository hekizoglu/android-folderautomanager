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

    // NOT: "benchmarkRelease" build type'ı burada elle tanımlanmaz — androidx.baselineprofile
    // eklentisi bunu :app modülündeki release build type'ından otomatik türetir (matchingFallbacks
    // dahil). Elle create("benchmarkRelease") eklemek AGP 8.6.1 varyant eşleşmesini bozuyor
    // (":app:mergeReleaseBaselineProfile" -> "No matching variant of project :benchmark" hatası).

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

baselineProfile {
    // Yerel makinede otomatik çalıştırma; APK üretiminden ayrı, elle tetiklenir:
    // .\gradlew :app:generateReleaseBaselineProfile -PallowDebugReleaseSigning=true
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.2.4")
}
