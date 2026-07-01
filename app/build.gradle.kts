import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Some transitive dependencies (notably Credential Manager's Play Services
// auth bridge) declare a loose version range for androidx.browser, which
// Gradle can resolve to a brand-new alpha/stable release that demands a
// compileSdk far ahead of this project's. Forcing a known-stable, known-
// compatible version here stops that drift. See:
// https://github.com/alchemyplatform/aa-sdk/issues/1534
configurations.all {
    resolutionStrategy {
        force("androidx.browser:browser:1.8.0")
    }
}

// Load local.properties (gitignored) so SUPABASE_URL etc. are available via
// project.findProperty() during local builds. CI builds pass these in
// directly as -P gradle properties instead, so this file is optional there.
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(FileInputStream(localFile))
    }
}
fun localOrProjectProperty(key: String): String =
    (localProperties.getProperty(key) ?: project.findProperty(key) as String?) ?: ""

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.samarth.logsapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.samarth.logsapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Supabase config is injected at build time via -P properties (see CI workflow)
        // and gradle.properties for local builds, so no secrets are hardcoded in source.
        buildConfigField(
            "String", "SUPABASE_URL",
            "\"${localOrProjectProperty("SUPABASE_URL")}\""
        )
        buildConfigField(
            "String", "SUPABASE_ANON_KEY",
            "\"${localOrProjectProperty("SUPABASE_ANON_KEY")}\""
        )
        buildConfigField(
            "String", "GOOGLE_WEB_CLIENT_ID",
            "\"${localOrProjectProperty("GOOGLE_WEB_CLIENT_ID")}\""
        )
        buildConfigField(
            "String", "GEMINI_API_KEY",
            "\"${localOrProjectProperty("GEMINI_API_KEY")}\""
        )
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Supabase-Kt (BOM keeps Auth/Postgrest/Ktor engine versions aligned).
    // Requires Kotlin 2.x — its kotlinx-serialization/kotlinx-io transitive
    // deps ship Kotlin 2.3-compiled metadata, which Kotlin 1.9.x cannot
    // read (hard compiler-level incompatibility, not a version range issue).
    // This is why the project's Kotlin version was bumped to 2.0.21 above.
    implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-okhttp:3.3.0")

    // Google Sign-In via Credential Manager (modern, no deprecated GoogleSignInClient)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // DataStore for local session/draft persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room — local sync ledger (NOT a content cache; tracks which .md files
    // have been pushed to Supabase yet). Pinned to stable 2.x; Room 3.0 is
    // KMP-focused and still alpha as of writing, so deliberately not used.
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Unit testing
    testImplementation("junit:junit:4.13.2")
}
