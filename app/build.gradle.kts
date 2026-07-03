import java.io.File
val releaseStorePath = providers.environmentVariable("V7L_KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("V7L_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("V7L_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("V7L_KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
    releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { !it.isNullOrBlank() }

gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { task ->
        task.path.contains("Release", ignoreCase = true) &&
            (task.name.startsWith("assemble") || task.name.startsWith("bundle"))
    }
    if (releaseRequested && !releaseSigningConfigured) {
        throw GradleException(
            "Release signing requires V7L_KEYSTORE_PATH, V7L_KEYSTORE_PASSWORD, " +
                "V7L_KEY_ALIAS and V7L_KEY_PASSWORD"
        )
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Package xray & tun2socks as native libs per ABI so they install under nativeLibraryDir (executable on strict OEMs).
val jniDir = layout.buildDirectory.dir("vpn-jni")

tasks.register("prepareVpnJniLibs") {
    outputs.dir(jniDir)
    doLast {
        val outRoot = jniDir.get().asFile
        if (outRoot.exists()) outRoot.deleteRecursively()
        outRoot.mkdirs()
        fun install(abiDir: String, src: File, libName: String) {
            if (!src.exists()) return
            val destDir = File(outRoot, abiDir)
            destDir.mkdirs()
            src.copyTo(File(destDir, libName), overwrite = true)
        }
        // Native engine binaries live under app/native-bin (NOT in
        // src/main/assets) so they are packaged exactly once — as per-ABI
        // jniLibs that ABI splits can strip down. Each binary is renamed to a
        // lib*.so so the platform extracts it to nativeLibraryDir.
        val binRoot = file("native-bin")
        // Xray
        install("arm64-v8a",   File(binRoot, "xray/xray-arm64"),   "libxray.so")
        install("armeabi-v7a", File(binRoot, "xray/xray-arm"),     "libxray.so")
        install("x86_64",      File(binRoot, "xray/xray-x86_64"),  "libxray.so")
        // sing-box (hyphenless lib name so every Android version extracts it)
        install("arm64-v8a",   File(binRoot, "singbox/sing-box-arm64"),   "libsingbox.so")
        install("armeabi-v7a", File(binRoot, "singbox/sing-box-arm"),     "libsingbox.so")
        install("x86_64",      File(binRoot, "singbox/sing-box-x86_64"),  "libsingbox.so")
        // tun2socks
        install("arm64-v8a",   File(binRoot, "tun2socks/tun2socks-arm64"),  "libtun2socks.so")
        install("armeabi-v7a", File(binRoot, "tun2socks/tun2socks-arm"),    "libtun2socks.so")
        install("x86_64",      File(binRoot, "tun2socks/tun2socks-x86_64"), "libtun2socks.so")
    }
}

tasks.named("preBuild").configure { dependsOn("prepareVpnJniLibs") }

android {
    namespace = "com.v7lthronyx.v7lpanel"
    compileSdk = 35

    sourceSets.named("main").configure {
        jniLibs.srcDir(jniDir)
    }

    // Build one APK per ABI so a device only ships its own engine binaries
    // (arm64-v8a covers virtually all phones; armeabi-v7a + x86_64 included for
    // older devices and emulators). Pick the matching APK when sideloading.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseStorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.v7lthronyx.v7lpanel"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // Core / Activity
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Ktor (HTTP client)
    implementation("io.ktor:ktor-client-okhttp:3.0.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")
    implementation("io.ktor:ktor-client-logging:3.0.2")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Room
    implementation("androidx.room:room-runtime:2.7.0-alpha12")
    implementation("androidx.room:room-ktx:2.7.0-alpha12")
    ksp("androidx.room:room-compiler:2.7.0-alpha12")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // QR Code (ZXing)
    implementation("com.google.zxing:core:3.5.3")

    // CameraX — for QR scanner
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.4.0-alpha02")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Koin DI
    implementation("io.insert-koin:koin-android:4.0.1")
    implementation("io.insert-koin:koin-androidx-compose:4.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
