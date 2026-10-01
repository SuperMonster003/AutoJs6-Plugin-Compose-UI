import com.android.apksig.ApkVerifier
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.compose.ui"
val buildTypeDebug = "debug"
val buildTypeRelease = "release"

// ---------------------------------------------------------------------------
// Host protocol AARs are consumed only from libs/ and are pinned by locks/host-api-aars.lock.
// The build refuses missing files, debug artifacts, placeholder hashes, extra lock entries and
// digest mismatches. Roadmap P0.1 stages common-plugin-api only; the Compose UI contract
// (compose-ui-api, host module plugin-api/compose-ui-api) joins this list as compileOnly once the
// P0.2 spike draft, then the frozen V1 of P1.1, exists (roadmap D9).
// ---------------------------------------------------------------------------

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun Properties.requiredValue(key: String): String =
    getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
        ?: error("Missing required lock value: $key")

fun File.loadUniqueLock(): Properties {
    val lock = Properties()
    useLines(Charsets.UTF_8) { lines ->
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#') || trimmed.startsWith('!')) {
                return@forEachIndexed
            }
            val separator = trimmed.indexOf('=')
            require(separator > 0) { "Malformed lock line ${index + 1} in $name" }
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            require(!lock.containsKey(key)) { "Duplicate lock key in $name: $key" }
            lock.setProperty(key, value)
        }
    }
    return lock
}

val sha256Pattern = Regex("[0-9a-f]{64}")

fun lockedAars(lockFile: File, ids: List<String>, directory: String): List<File> {
    require(lockFile.isFile) { "Missing AAR lock: ${lockFile.relativeTo(rootProject.projectDir)}" }
    val lock = lockFile.loadUniqueLock()
    val expectedKeys = setOf("format") + ids.flatMap { id -> listOf("$id.file", "$id.sha256") }
    require(lock.stringPropertyNames() == expectedKeys) {
        "${lockFile.name} must contain exactly these keys: ${expectedKeys.sorted()}"
    }
    require(lock.requiredValue("format") == "1") { "Unsupported lock format in ${lockFile.name}" }
    return ids.map { id ->
        val fileName = lock.requiredValue("$id.file")
        val expectedSha256 = lock.requiredValue("$id.sha256").lowercase()
        require(fileName == File(fileName).name && fileName.endsWith(".aar")) { "Invalid $id.file in ${lockFile.name}" }
        require(!fileName.endsWith("-debug.aar")) { "Debug AARs are forbidden: $fileName" }
        require(sha256Pattern.matches(expectedSha256)) {
            "Replace $id.sha256 in ${lockFile.name} with the audited release AAR SHA-256 before Gradle configuration"
        }
        val artifact = rootProject.file("$directory/$fileName")
        require(artifact.isFile) { "Missing locked AAR: ${artifact.relativeTo(rootProject.projectDir)}" }
        val actualSha256 = artifact.sha256()
        require(actualSha256 == expectedSha256) {
            "SHA-256 mismatch for $fileName: expected $expectedSha256, actual $actualSha256"
        }
        artifact
    }
}

// Host contract AARs: common-plugin-api from the host 6.8.0 snapshot 77b5a3b0c5 (build 5307), see libs/README.md.
val hostApiIds = listOf("common-plugin-api")
val hostApiAars = lockedAars(rootProject.file("locks/host-api-aars.lock"), hostApiIds, "libs")

android {
    // The host can select any bundled locale independently of the Android system language.
    bundle { language { enableSplit = false } }
    // Keep only the plugin's 10 languages from the Compose / AndroidX resources that the renderer resolves (roadmap D11).
    androidResources { localeFilters += setOf("en", "ar", "es", "fr", "ja", "ko", "ru", "zh", "zh-rCN", "zh-rHK", "zh-rTW") }
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_engine", "compose")
        resValue("string", "plugin_id", "compose-ui")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    lint {
        abortOnError = true
        // Product text intentionally uses ASCII punctuation in every locale.
        disable += "TypographyEllipsis"
        // core-ktx arrives only transitively (and at run time from the host, roadmap D10); the code base keeps plain platform APIs.
        disable += "UseKtx"
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            // R8 with the Compose consumer rules plus proguard-rules.pro (keeps the renderer factory and the contract types, roadmap D22).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        resValues = true
    }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
    }

    // No ABI splits on purpose (roadmap D22): the plugin is pure bytecode plus resources (Compose ships no
    // native library), so a split would produce identical APKs. Every device installs the same single APK and
    // getInfo() reports supportedAbis = emptyArray(); appendDigestToReleasedFiles rejects any lib/ entry.
    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    "${rootProject.name}-v$version.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    // PluginInfo, IPluginInfoProvider and the shared plugin constants (host module plugin-api/common-plugin-api).
    implementation(files(hostApiAars))

    // Jetpack Compose (roadmap D25): the renderer classes live only in this APK; the host loads them through a
    // PathClassLoader whose parent is the host class loader (roadmap D10). AndroidX core / activity / lifecycle /
    // savedstate arrive transitively here and are shared with the host at run time; roadmap P0.2 / D26 pins them.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.ext.junit)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register("appendDigestToReleasedFiles") {
        group = "distribution"
        description = "Builds and verifies the single signed release APK of Compose UI, then appends its CRC32 digest"
        dependsOn("assembleRelease")

        doLast {
            check(signs.isValid) { "Release signing is not configured (sign.properties); refusing to collect an unsigned APK" }
            val extension = utils.FILE_EXTENSION_APK
            val prefix = "${rootProject.name}-v${versions.appVersionName.replace("\\s".toRegex(), "-")}".lowercase()
            val expectedName = "$prefix.$extension"
            val source = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease").get().asFile
            val apks = source.listFiles { candidate -> candidate.extension == extension }.orEmpty()
            check(apks.map { it.name } == listOf(expectedName)) {
                "Unexpected release APK set in $source: expected [$expectedName], actual ${apks.map { it.name }.sorted()}"
            }
            val apk = apks.single()
            val verification = ApkVerifier.Builder(apk).build().verify()
            check(verification.isVerified) { "Invalid or unsigned release APK ${apk.name}: ${verification.errors}" }
            ZipFile(apk).use { zip ->
                // Roadmap D22: pure bytecode, no native library, hence no ABI split.
                val nativeLibraries = zip.entries().asSequence().map { it.name }.filter { it.startsWith("lib/") }.toList()
                check(nativeLibraries.isEmpty()) { "The plugin must not package native libraries, found $nativeLibraries" }
            }
            val collectedName = "$prefix-${utils.digestCRC32(apk)}.$extension"
            val destination = file("$rootDir/${buildTypeRelease}s")
            check(destination.isDirectory || destination.mkdirs()) { "Cannot create $destination" }
            apk.copyTo(destination.resolve(collectedName), overwrite = true)
            // Only retire superseded artifacts of this exact version after the input verified.
            destination.listFiles { file -> file.name.startsWith("$prefix-") && file.extension == extension }
                .orEmpty().filter { it.name != collectedName }.forEach { stale ->
                    check(stale.delete()) { "Cannot remove superseded artifact: $stale" }
                }
            println("Destination: $destination")
            println(collectedName)
        }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}
