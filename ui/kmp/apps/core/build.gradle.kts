plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.compose)
    alias(libs.plugins.local.dependencies.compose)
    alias(libs.plugins.local.dependencies.serialization)
    alias(libs.plugins.local.dependencies.koin)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        androidMain.dependencies {
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

            // Lifecycle
            implementation(libs.bundles.androidx.lifecycle.compose)

            // Navigation
            implementation(libs.navigation.compose)

            // Ktor
            implementation(libs.bundles.ktor.client)
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
