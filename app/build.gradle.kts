import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Copies the built APK(s) to `build/dist/` under a friendly, versioned name.
 * AGP 9 removed the old `outputFileName` API, so the supported way to rename an
 * APK is to listen to the APK artifact and copy it (see the official
 * gradle-recipes `listenToArtifacts` example).
 */
abstract class RenameApk : DefaultTask() {

    @get:InputDirectory
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @TaskAction
    fun taskAction() {
        val outDir = output.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()
        val built = builtArtifactsLoader.get().load(input.get()) ?: return
        built.elements.forEach { artifact ->
            val version = artifact.versionName?.takeIf { it.isNotBlank() } ?: "0.0.0"
            File(artifact.outputFile).copyTo(
                File(outDir, "OpenLauncher_Custom-v$version.apk"),
                overwrite = true
            )
        }
    }
}

android {
    namespace  = "com.openlauncher.app"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        // Fork identity: unique package so this build never collides with the
        // original OpenLauncher or other forks on the same device, and so the
        // Google Maps API key restriction is ours alone.
        applicationId  = "com.limbo.openlauncher"
        minSdk         = 21
        targetSdk      = 34
        versionCode    = 119
        versionName    = "0.9.119"

        // Google Maps key is NOT stored in the repo. Put it in local.properties
        // (git-ignored) as:  MAPS_API_KEY=AIza...
        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val mapsKey = localProps.getProperty("MAPS_API_KEY")
            ?: (project.findProperty("MAPS_API_KEY") as String?)
            ?: "REPLACE_WITH_YOUR_MAPS_API_KEY"
        manifestPlaceholders["mapsApiKey"] = mapsKey
    }

    signingConfigs {
        create("release") {
            // PRIVATE fork key — stored OUTSIDE the repo (C:\Users\admin\.android-keys\).
            // Back this file up; losing it means the app can never be updated
            // in place again. Other forks cannot sign over this app.
            // Secrets (alias + passwords) live in keystore.properties (git-ignored).
            val ks = Properties().apply {
                val f = rootProject.file("keystore.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
            storeFile = file(ks.getProperty("storeFile") ?: "C:/Users/admin/.android-keys/limbo-openlauncher.jks")
            storePassword = ks.getProperty("storePassword") ?: ""
            keyAlias = ks.getProperty("keyAlias") ?: "limbo"
            keyPassword = ks.getProperty("keyPassword") ?: ""
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            // Default signing config for normal device testing (restores app visibility)
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // AGP 9.1.1's lint crashes on Kotlin 2.2.10 FIR contract resolution
        // ("Unexpected failure during lint analysis ... from CONTRACTS to
        // ANNOTATION_ARGUMENTS", e.g. while analyzing OnboardingScreen.kt).
        // That is a lint/K2 infrastructure bug, not a code issue; lintVital only
        // gates release builds on a small subset of checks, so disable it.
        checkReleaseBuilds = false
    }
}

// Name the output APK after the app + version. This only copies the built APK
// under a friendlier file name — package identity, versionCode/versionName and
// signing are all unaffected.
androidComponents {
    onVariants(selector().all()) { variant ->
        val renameTask = tasks.register<RenameApk>("renameApk${variant.name.replaceFirstChar { it.uppercase() }}") {
            output.set(layout.buildDirectory.dir("dist"))
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
        }
        variant.artifacts.use(renameTask).wiredWith { it.input }.toListenTo(SingleArtifact.APK)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Network — weather
    implementation("com.squareup.retrofit2:retrofit:2.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.12.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")

    // Google Maps widget (official SDK — API key lives in AndroidManifest.xml)
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // OSM map provider (osmdroid) — free tiles, no key, offline cache
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Lottie animation (Meteocons)
    implementation("com.airbnb.android:lottie-compose:6.6.2")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.13.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
