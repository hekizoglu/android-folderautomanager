import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val skipGoogleServices = project.hasProperty("skipGoogleServices")

if (skipGoogleServices) {
    println("[ci] skipGoogleServices aktif — Google Services islemleri atlaniyor")
    afterEvaluate {
        tasks.whenTaskAdded {
            if (name.startsWith("process") && name.contains("GoogleServices", ignoreCase = true)) {
                enabled = false
                actions = listOf()
            }
        }
    }
} else {
    plugins.apply("com.google.gms.google-services")
    plugins.apply("com.google.firebase.crashlytics")
}

val keystoreProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

android {
    namespace = "com.armutlu.apporganizer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.armutlu.apporganizer"
        minSdk = 26
        targetSdk = 35
        versionCode = 28
        versionName = "1.3.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] as? String ?: "release.jks")
            storePassword = keystoreProps["storePassword"] as? String ?: ""
            keyAlias = keystoreProps["keyAlias"] as? String ?: "apporganizer"
            keyPassword = keystoreProps["keyPassword"] as? String ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.layout.buildDirectory.get()}/compose_compiler",
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.layout.buildDirectory.get()}/compose_compiler"
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = false
            all { test ->
                test.jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
            }
        }
    }

}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

// Workaround: local JVM test class discovery/runtime breaks when directory-based classpath
// entries live under a non-ASCII workspace path (ör. "Klasörleri"). We mirror the
// relevant test class directories into an ASCII-only temp folder and point Gradle's
// test scanning/runtime at those copies.
afterEvaluate {
    val asciiTestClasspathRoot = File(System.getProperty("java.io.tmpdir"), "apporganizer-test-classpath")
    val syncDebugAsmDirs = tasks.register<Sync>("syncDebugAsmClassesAscii") {
        dependsOn("transformDebugClassesWithAsm")
        from(layout.buildDirectory.dir(
            "intermediates/classes/debug/transformDebugClassesWithAsm/dirs"
        ))
        into(File(asciiTestClasspathRoot, "debug-asm-classes"))
        include("**/*.class")
    }
    val syncDebugKotlinClasses = tasks.register<Sync>("syncDebugKotlinClassesAscii") {
        dependsOn("compileDebugKotlin")
        from(layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
        into(File(asciiTestClasspathRoot, "debug-kotlin-classes"))
        include("**/*.class")
    }
    val syncDebugJavaClasses = tasks.register<Sync>("syncDebugJavaClassesAscii") {
        dependsOn("compileDebugJavaWithJavac")
        from(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes"))
        into(File(asciiTestClasspathRoot, "debug-java-classes"))
        include("**/*.class")
    }
    val syncAsmDirs = tasks.register<Sync>("syncHiltAsmTestClassesAscii") {
        dependsOn("transformDebugUnitTestClassesWithAsm")
        from(layout.buildDirectory.dir(
            "intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs"
        ))
        into(File(asciiTestClasspathRoot, "hilt-asm-test-classes"))
        include("**/*.class")
    }
    val syncUnitTestKotlinClasses = tasks.register<Sync>("syncDebugUnitTestKotlinClassesAscii") {
        dependsOn("compileDebugUnitTestKotlin")
        from(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"))
        into(File(asciiTestClasspathRoot, "debug-unit-test-kotlin-classes"))
        include("**/*.class")
    }
    val syncUnitTestJavaClasses = tasks.register<Sync>("syncDebugUnitTestJavaClassesAscii") {
        dependsOn("compileDebugUnitTestJavaWithJavac")
        from(layout.buildDirectory.dir("intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes"))
        into(File(asciiTestClasspathRoot, "debug-unit-test-java-classes"))
        include("**/*.class")
    }
    tasks.withType<Test>().configureEach {
        dependsOn(
            syncDebugAsmDirs,
            syncDebugKotlinClasses,
            syncDebugJavaClasses,
            syncAsmDirs,
            syncUnitTestKotlinClasses,
            syncUnitTestJavaClasses
        )
        val debugAsmDir = File(asciiTestClasspathRoot, "debug-asm-classes")
        val debugKotlinDir = File(asciiTestClasspathRoot, "debug-kotlin-classes")
        val debugJavaDir = File(asciiTestClasspathRoot, "debug-java-classes")
        val asmDir = File(asciiTestClasspathRoot, "hilt-asm-test-classes")
        val kotlinTestDir = File(asciiTestClasspathRoot, "debug-unit-test-kotlin-classes")
        val javaTestDir = File(asciiTestClasspathRoot, "debug-unit-test-java-classes")
        testClassesDirs = files(kotlinTestDir, javaTestDir)
        classpath =
            files(debugAsmDir, debugKotlinDir, debugJavaDir, asmDir, kotlinTestDir, javaTestDir) +
            classpath
    }
}

dependencies {
    // Color picker
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")

    // Biometric (Settings Lock)
    implementation("androidx.biometric:biometric:1.1.0")

    // Palette (dominant color extraction)
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Bundled SQLite with FTS5 — platform SQLite'da FTS5 eksikse fallback LIKE kullanılır

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Firebase — Analytics + Crashlytics + Messaging (FCM push)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // WorkManager — zamanlanmis yedekleme gorevi
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // Coil — async image loading (uygulama ikonu) — coil3 compileSdk36 gerektirir, coil2 kullan
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.android.material:material:1.11.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:2.6.1")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // LeakCanary unit test'te ASM transform ile classpath wiring bozuyor — kaldırıldı
    // debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
