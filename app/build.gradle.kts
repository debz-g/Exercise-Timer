import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release key, read from the gitignored keystore.properties in the project root.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val hasReleaseKey = keystoreProperties.getProperty("storeFile") != null

// The version comes from the command line so shipping an APK never needs a source edit:
//   ./gradlew :app:releaseApk -PversionCode=2 -PversionName=1.1
// The defaults are for debug builds only.
val appVersionCode = providers.gradleProperty("versionCode").map { it.toIntOrNull() ?: error("versionCode must be a number, was '$it'") }.orNull ?: 1
val appVersionName = providers.gradleProperty("versionName").orNull ?: "1.0"

android {
    namespace = "com.exercise.timer"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "com.exercise.timer"
        // Obsidian's frosted glass needs Android 12.
        minSdk = 31
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: shrink, optimise and obfuscate code, then drop unused resources. Hilt, Compose,
            // DataStore and Obsidian ship their own consumer rules; app rules are in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // The release key keeps every future APK installable over the one on Dad's phone;
            // falls back to the debug key on a machine without keystore.properties.
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.obsidian) // brings Compose UI, Material 3 and Haze
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

// Keeps the UI on the Obsidian design system (from Obsidian's docs/NEW_APP.md).
val verifyDesignRules = tasks.register("verifyDesignRules") {
    group = "verification"
    val sources = fileTree("src/main") { include("**/*.kt") }
    inputs.files(sources)
    doLast {
        val rules = listOf(
            Regex("""\bMaterialTheme\.""") to "use ObsidianTheme.colors / typography",
            Regex("""\.(combinedC|c)lickable\s*[({]""") to "use Modifier.pressable { }",
            Regex("""\bripple\s*\(""") to "no ripples; use Modifier.pressable",
            Regex("""\bColor\s*\(\s*0x""") to "hard-coded colour; use a token",
        )
        val problems = sources.files.sortedBy { it.path }.flatMap { file ->
            file.readLines().flatMapIndexed { i, line ->
                val code = line.substringBefore("//")
                rules.filter { (pattern, _) -> pattern.containsMatchIn(code) }
                    .map { (_, why) -> "${file.path}:${i + 1}: $why" }
            }
        }
        if (problems.isNotEmpty()) throw GradleException("Obsidian design rules:\n" + problems.joinToString("\n"))
    }
}
tasks.named("check") { dependsOn(verifyDesignRules) }

// One command for a shareable APK: build it signed and drop it in the project root as
// ExerciseTimer-<version>.apk, which is where every copy handed to Dad has come from.
// Not a Copy task: its destination directory would be the project root, which AGP already reads as an
// output of its own tasks.
val releaseApk = tasks.register("releaseApk") {
    group = "build"
    description = "Builds the release APK and copies it to the project root as ExerciseTimer-$appVersionName.apk"
    val builtApk = layout.buildDirectory.dir("outputs/apk/release").get().asFile.resolve("app-release.apk")
    val shareableApk = rootDir.resolve("ExerciseTimer-$appVersionName.apk")
    dependsOn("assembleRelease")
    inputs.file(builtApk)
    outputs.file(shareableApk)
    doLast {
        builtApk.copyTo(shareableApk, overwrite = true)
    }
}

