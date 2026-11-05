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
            baseName = "AppsCore"
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
        namespace = "dev.fnc314.tools.jobtracker.apps.core"
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
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.bundles.compose.tooling)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.bundles.ktor.client.android)
            implementation(libs.bundles.firebase.android)
            implementation(libs.kotlinx.coroutinesPlayServices)
        }
        commonMain.dependencies {
            api(projects.coreData)
            implementation(projects.coreNetwork)
            implementation(projects.coreDesignSystem)
            implementation(projects.coreWidgets)

            implementation(projects.featuresSignInUi)
            implementation(projects.featuresJobApplicationsListUi)
            implementation(projects.featuresJobApplicationsDetailsUi)
            implementation(projects.featuresSettingsUi)
            implementation(projects.featuresAddJobApplicationUi)
            implementation(projects.featuresTrackerEditsUi)

            // Compose UI
            implementation(libs.bundles.compose.ui)

            // Lifecycle
            implementation(libs.bundles.androidx.lifecycle.compose)

            // Koin
            implementation(libs.bundles.koin)

            // Navigation
            implementation(libs.navigation.compose)

            // Ktor & Serialization
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
            implementation(libs.bundles.firebase.jvm)
            implementation(libs.records.proto)
            implementation(libs.records.parser)
        }
        iosMain.dependencies {
            implementation(libs.bundles.ktor.client.ios)
        }
    }
}
