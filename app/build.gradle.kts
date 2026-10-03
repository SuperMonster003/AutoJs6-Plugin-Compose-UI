import com.android.apksig.ApkVerifier
import org.gradle.api.provider.Property
import com.android.build.api.variant.BuildConfigField
import java.security.MessageDigest
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("jacoco")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.compose.ui"
val buildTypeDebug = "debug"
val buildTypeRelease = "release"
val composeUiCoverage = providers.gradleProperty("composeUiCoverage").map(String::toBoolean).getOrElse(false)

// ---------------------------------------------------------------------------
// Host protocol AARs are consumed only from libs/ and are pinned by locks/host-api-aars.lock.
// The build refuses missing files, debug artifacts, placeholder hashes, extra lock entries and
// digest mismatches. common-plugin-api is bundled for INFO; compose-ui-api is a compile-only
// V1 contract. No implementation references the retained legacy api.spike types.
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
val hostApiIds = listOf("common-plugin-api", "compose-ui-api")
val hostApiAars = lockedAars(rootProject.file("locks/host-api-aars.lock"), hostApiIds, "libs")

// D26: these classes are supplied by the production host. Keep Kotlin in the APK for INFO/Wake,
// which execute outside the host, while parent-first loading shares the host's Kotlin in rendering.
val sharedDeps = rootProject.file("locks/host-shared-deps.lock").loadUniqueLock()
    .entries.associate { it.key.toString() to it.value.toString() }
require(sharedDeps.isNotEmpty() && sharedDeps.keys.none { it.startsWith("androidx.compose:") || it.startsWith("androidx.compose.") })
val sharedFingerprint = MessageDigest.getInstance("SHA-256")
    .digest(sharedDeps.toSortedMap().entries.joinToString("") { "${it.key}=${it.value}\n" }.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
configurations.matching { it.name in setOf("debugRuntimeClasspath", "releaseRuntimeClasspath") }.configureEach {
    sharedDeps.keys.filterNot { it.startsWith("org.jetbrains.kotlin:") }.forEach { coordinate ->
        exclude(group = coordinate.substringBefore(':'), module = coordinate.substringAfter(':'))
    }
}

// Native code inventory (roadmap D22, amended by the P0.1 evidence): the plugin has no native code of its own,
// but Compose ui-graphics depends on androidx.graphics:graphics-path, whose libandroidx.graphics.path.so is
// packaged for the four Android ABIs. The APK stays one universal package without ABI splits; the release gate
// verifies that exactly these libraries are present, stored uncompressed, and 16 KB page-aligned (ELF PT_LOAD
// and zip data offset) so that 16 KB page devices can map them straight from the APK.
val nativeAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
val allowedNativeLibraries = listOf("libandroidx.graphics.path.so")
val nativePageAlignment = 16384L

fun elfMinimumLoadAlignment(bytes: ByteArray): Long {
    check(bytes.size > 0x40 && bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
        "Not an ELF file"
    }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val is64 = bytes[4].toInt() == 2
    val programHeaderOffset = if (is64) buffer.getLong(0x20) else (buffer.getInt(0x1C).toLong() and 0xFFFFFFFFL)
    val entrySize = buffer.getShort(if (is64) 0x36 else 0x2A).toInt() and 0xFFFF
    val entryCount = buffer.getShort(if (is64) 0x38 else 0x2C).toInt() and 0xFFFF
    var minimum = Long.MAX_VALUE
    repeat(entryCount) { index ->
        val offset = (programHeaderOffset + index.toLong() * entrySize).toInt()
        if (buffer.getInt(offset) == 1) { // PT_LOAD
            val alignment = if (is64) buffer.getLong(offset + 48) else (buffer.getInt(offset + 28).toLong() and 0xFFFFFFFFL)
            minimum = minOf(minimum, alignment)
        }
    }
    check(minimum != Long.MAX_VALUE) { "No PT_LOAD segment" }
    return minimum
}

/** Data offsets of the `lib/` entries inside [apk], read from the central directory and local headers. */
fun nativeEntryDataOffsets(apk: File): Map<String, Long> = RandomAccessFile(apk, "r").use { file ->
    val length = file.length()
    val tailLength = minOf(length, 22L + 65535L).toInt()
    val tail = ByteArray(tailLength).also { file.seek(length - tailLength); file.readFully(it) }
    val tailBuffer = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
    val eocd = (tail.size - 22 downTo 0).firstOrNull { tailBuffer.getInt(it) == 0x06054b50 }
    checkNotNull(eocd) { "End of central directory not found in ${apk.name}" }
    val entryCount = tailBuffer.getShort(eocd + 10).toInt() and 0xFFFF
    val directorySize = tailBuffer.getInt(eocd + 12).toLong() and 0xFFFFFFFFL
    val directoryOffset = tailBuffer.getInt(eocd + 16).toLong() and 0xFFFFFFFFL
    val directory = ByteArray(directorySize.toInt()).also { file.seek(directoryOffset); file.readFully(it) }
    val directoryBuffer = ByteBuffer.wrap(directory).order(ByteOrder.LITTLE_ENDIAN)
    val offsets = LinkedHashMap<String, Long>()
    var position = 0
    repeat(entryCount) {
        check(directoryBuffer.getInt(position) == 0x02014b50) { "Corrupt central directory in ${apk.name}" }
        val nameLength = directoryBuffer.getShort(position + 28).toInt() and 0xFFFF
        val extraLength = directoryBuffer.getShort(position + 30).toInt() and 0xFFFF
        val commentLength = directoryBuffer.getShort(position + 32).toInt() and 0xFFFF
        val localHeaderOffset = directoryBuffer.getInt(position + 42).toLong() and 0xFFFFFFFFL
        val name = String(directory, position + 46, nameLength, Charsets.UTF_8)
        if (name.startsWith("lib/")) {
            val local = ByteArray(30).also { file.seek(localHeaderOffset); file.readFully(it) }
            val localBuffer = ByteBuffer.wrap(local).order(ByteOrder.LITTLE_ENDIAN)
            check(localBuffer.getInt(0) == 0x04034b50) { "Corrupt local header for $name" }
            val localNameLength = localBuffer.getShort(26).toInt() and 0xFFFF
            val localExtraLength = localBuffer.getShort(28).toInt() and 0xFFFF
            offsets[name] = localHeaderOffset + 30 + localNameLength + localExtraLength
        }
        position += 46 + nameLength + extraLength + commentLength
    }
    offsets
}

fun verifyNativeLibraries(apk: File) {
    val expected = nativeAbis.flatMap { abi -> allowedNativeLibraries.map { "lib/$abi/$it" } }.toSet()
    ZipFile(apk).use { zip ->
        val entries = zip.entries().asSequence().filter { it.name.startsWith("lib/") }.toList()
        check(entries.map { it.name }.toSet() == expected) {
            "Unexpected native library set in ${apk.name}: expected $expected, actual ${entries.map { it.name }.sorted()}"
        }
        entries.forEach { entry ->
            check(entry.method == ZipEntry.STORED) { "${entry.name} must be stored uncompressed so it can be mapped from the APK" }
            val alignment = elfMinimumLoadAlignment(zip.getInputStream(entry).use { it.readBytes() })
            check(alignment >= nativePageAlignment) { "${entry.name} PT_LOAD alignment $alignment is below $nativePageAlignment" }
        }
    }
    nativeEntryDataOffsets(apk).forEach { (name, offset) ->
        check(offset % nativePageAlignment == 0L) { "$name data offset $offset is not $nativePageAlignment-byte aligned in the APK" }
    }
}

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
        buildConfigField("String", "SHARED_DEPS_FINGERPRINT", "\"$sharedFingerprint\"")

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
            enableAndroidTestCoverage = composeUiCoverage
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

    testCoverage { jacocoVersion = libs.versions.jacoco.get() }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
    }

    // No ABI splits on purpose (roadmap D22): the only native code is the ~10 KB androidx graphics-path helper
    // that Compose ui-graphics brings for every ABI (see nativeAbis / allowedNativeLibraries above), so splitting
    // would save nothing measurable. Every device installs the same single APK and getInfo() reports
    // supportedAbis = emptyArray(); appendDigestToReleasedFiles verifies the exact native library set and alignment.
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
        requireNotNull(variant.buildConfigFields).put("COMPOSE_VERSION", providers.provider {
            val runtime = configurations.getByName("${variant.name}RuntimeClasspath").incoming.resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }.single { it.group == "androidx.compose.runtime" && it.name == "runtime-android" }
            BuildConfigField("String", "\"${runtime.version}\"", "Resolved from the Compose BOM; not a second version pin.")
        })
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
    implementation(files(hostApiAars[0]))
    compileOnly(files(hostApiAars[1]))
    testImplementation(files(hostApiAars[1]))
    androidTestImplementation(files(hostApiAars[1]))
    sharedDeps.forEach { (coordinate, version) ->
        if (coordinate.startsWith("org.jetbrains.kotlin:")) {
            implementation("$coordinate:$version")
        } else {
            compileOnly("$coordinate:$version")
            testImplementation("$coordinate:$version")
            androidTestImplementation("$coordinate:$version")
        }
    }

    // Jetpack Compose (roadmap D25): the renderer classes live only in this APK; the host loads them through a
    // PathClassLoader whose parent is the host class loader (roadmap D10). AndroidX core / activity / lifecycle /
    // savedstate are compile-only through the host shared lock above (roadmap P0.2 / D26).
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
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

tasks {
    register("verifySharedClasspath") {
        group = "verification"
        doLast {
            listOf("debug", "release").forEach { variant ->
                val compile = configurations.getByName("${variant}CompileClasspath").incoming.resolutionResult.allComponents
                    .mapNotNull { it.moduleVersion }.associate { "${it.group}:${it.name}" to it.version }
                val runtime = configurations.getByName("${variant}RuntimeClasspath").incoming.resolutionResult.allComponents
                    .mapNotNull { it.moduleVersion }.associate { "${it.group}:${it.name}" to it.version }
                sharedDeps.forEach { (id, version) ->
                    check(compile[id] == version) { "$variant $id compiled against ${compile[id]}, expected host $version" }
                    if (!id.startsWith("org.jetbrains.kotlin:")) check(id !in runtime) { "$variant bundles host component $id" }
                }
            }
            println("Shared classpath verified: ${sharedDeps.size} components, SHA-256 $sharedFingerprint")
        }
    }
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
            // Roadmap D22 (amended): no first-party native code; only the Compose graphics-path helper, 16 KB aligned.
            verifyNativeLibraries(apk)
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

jacoco { toolVersion = libs.versions.jacoco.get() }

// adb runs use explicit owned serials; collected .ec files can be reported without discovering
// or installing on any other connected device. These are the original, uninstrumented classes.
tasks.register<JacocoReport>("reportComposeUiDeviceCoverage") {
    group = "verification"
    description = "Reports plugin-source coverage from explicitly collected device execution data"
    dependsOn("bundleDebugClassesToCompileJar")
    executionData.from(fileTree(layout.buildDirectory.dir("outputs/compose-coverage")) { include("*.ec") })
    classDirectories.from(zipTree(layout.buildDirectory.file(
        "intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar",
    )).matching {
        include("io/github/supermonster003/autojs6/plugin/compose/ui/**")
        exclude("**/BuildConfig.class")
    })
    sourceDirectories.from("src/main/java")
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/compose-coverage/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/compose-coverage/coverage.xml"))
        csv.outputLocation.set(layout.buildDirectory.file("reports/compose-coverage/coverage.csv"))
    }
    setOnlyIf {
        check(composeUiCoverage) { "Build and report with -PcomposeUiCoverage=true" }
        check(executionData.files.any { it.isFile && it.length() > 0 }) { "No collected device coverage in outputs/compose-coverage" }
        true
    }
}
