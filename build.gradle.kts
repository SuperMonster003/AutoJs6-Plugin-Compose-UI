// @Hint: declared here, applied by the modules.
//  ! These plugins used to arrive through the settings buildscript classpath, which the
//  ! platform-versions plugin replaces. Declaring them once here keeps the version in a
//  ! single place and leaves the module scripts untouched.
//  ! The Compose compiler plugin ships with every Kotlin release since Kotlin 2.0, so it
//  ! follows the Kotlin version the platform plugin selected (roadmap D25); never hardcode it.
//  ! zh-CN: 这些插件原先经由 settings buildscript classpath 提供, 现已被 platform-versions 插件取代.
//  ! 在此声明一次可使版本只出现在一处, 模块脚本无须改动.
//  ! Compose 编译器插件自 Kotlin 2.0 起随 Kotlin 发布, 因此跟随平台插件选定的 Kotlin 版本 (路线图 D25), 不硬编码.
plugins {
    id("com.android.application") version System.getProperty("gradle.agp.version") apply false
    id("org.jetbrains.kotlin.plugin.compose") version System.getProperty("gradle.kotlin.version") apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/jcenter")
        maven("https://maven.aliyun.com/repository/public")
    }
}

tasks {
    register<Delete>("clean").configure {
        delete(rootProject.layout.buildDirectory)
    }
}
