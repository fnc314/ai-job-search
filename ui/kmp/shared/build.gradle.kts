import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    jvm()
    js {
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    android {
       namespace = "dev.fnc314.tools.jobtracker.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.bundles.compose.tooling)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
        }
        commonMain.dependencies {
            implementation(projects.coreWidgets)
            // Compose UI
            implementation(libs.bundles.compose.ui)
            
            // Lifecycle
            implementation(libs.bundles.androidx.lifecycle.compose)
            
            // Koin
            implementation(libs.bundles.koin)
            
            // Navigation
            implementation(libs.navigation.compose)
            
            // Ktor
            implementation(libs.bundles.ktor.client)
            implementation(libs.bundles.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.bundles.ktor.client.js)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.ktor.client.jvm)
            // Firebase Admin SDK for JVM (Desktop)
            implementation(libs.bundles.firebase.jvm)
        }
        androidMain.dependencies {
            implementation(libs.bundles.ktor.client.android)
            // Firebase
            implementation(libs.bundles.firebase.android)
        }
        iosMain.dependencies {
            implementation(libs.bundles.ktor.client.ios)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}