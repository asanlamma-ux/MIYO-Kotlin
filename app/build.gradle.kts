plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun org.gradle.api.Project.secret(name: String): String =
    providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: ""

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.miyu.reader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.miyu.reader"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "SUPABASE_URL", buildConfigString(project.secret("MIYU_SUPABASE_URL")))
        buildConfigField("String", "SUPABASE_ANON_KEY", buildConfigString(project.secret("MIYU_SUPABASE_ANON_KEY")))

        ndkVersion = "26.1.10909125"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O2", "-fexceptions", "-frtti")
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_TOOLCHAIN=clang",
                )
            }
        }
    }

    signingConfigs {
        create("release") {
            val customKeystore = System.getenv("MIYU_KEYSTORE")
            val customStorePass = System.getenv("MIYU_STORE_PASSWORD")
            val customKeyAlias = System.getenv("MIYU_KEY_ALIAS")
            val customKeyPass = System.getenv("MIYU_KEY_PASSWORD")

            if (customKeystore != null &&
                file(customKeystore).exists() &&
                !customStorePass.isNullOrEmpty() &&
                !customKeyAlias.isNullOrEmpty()
            ) {
                storeFile = file(customKeystore)
                storePassword = customStorePass
                keyAlias = customKeyAlias
                keyPassword = customKeyPass ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // R8: Enable more aggressive optimizations
            // Note: Full mode can be enabled via gradle.properties: android.enableR8.fullMode=true
        }
        debug {
            isDebuggable = true
            isJniDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkAllWarnings = true
        warningsAsErrors = false
        disable += listOf("InvalidPackage")
    }

    // =========================================================================
    // PRIORITY 2 (HIGH): R8/ProGuard Configuration
    // =========================================================================

    // Enable R8 full mode for better optimization (AGP 8.0+)
    // android.enableR8.fullMode=true in gradle.properties is recommended

    // Disable R8 to generate R8 diagnostics if build still fails:
    // - Run: ./gradlew assembleRelease --proguard-rules=diagnostic.pro
    // - See: https://developer.android.com/build/shrink-code#strict-reflection

    // Alternative: Use R8 in relaxed mode for debugging:
    // buildTypes.release { postprocessing { isRemoveCode = false } }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += setOf("**/libc++_shared.so", "**/libmmkv.so")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Room DB
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // MMKV (fast key-value storage)
    implementation("com.tencent:mmkv:1.3.5")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.4.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // Ktor (HTTP client for sync/auth)
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    implementation("io.ktor:ktor-client-logging:2.3.11")

    // HTML parsing for OPDS-adjacent online novel provider imports
    implementation("org.jsoup:jsoup:1.17.2")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // WebView (for reader rendering)
    implementation("androidx.webkit:webkit:1.11.0")

    // Accompanist (system UI, insets, etc.)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Lottie animations
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // SLF4J Android binding (Ktor logging support)
    implementation("org.slf4j:slf4j-android:1.7.36")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.11")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
