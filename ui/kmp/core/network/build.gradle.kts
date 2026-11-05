import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CoreNetwork"
            isStatic = true
        }
    }
    jvm()
    js {
        browser()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    android {
        namespace = "dev.fnc314.tools.jobtracker.core.network"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.ktor.client)
        }
        androidMain.dependencies {
            implementation(libs.bundles.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.bundles.ktor.client.ios)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.ktor.client.jvm)
        }
        jsMain.dependencies {
            implementation(libs.bundles.ktor.client.js)
        }
    }
}
