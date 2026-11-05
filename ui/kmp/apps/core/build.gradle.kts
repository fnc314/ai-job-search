plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.compose)
    alias(libs.plugins.local.dependencies.compose)
    alias(libs.plugins.local.dependencies.serialization)
    alias(libs.plugins.local.dependencies.di)
    alias(libs.plugins.local.dependencies.navigation)
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
        commonMain.dependencies {
            implementation(projects.coreData)
            implementation(projects.coreNetwork)
            implementation(projects.coreNavigation)
            implementation(projects.coreDesignSystem)
            implementation(projects.coreWidgets)

            implementation(projects.featuresSignInUi)
            implementation(projects.featuresSignInDomain)
            implementation(projects.featuresJobApplicationsListUi)
            implementation(projects.featuresJobApplicationsDetailsUi)
            implementation(projects.featuresSettingsUi)
            implementation(projects.featuresAddJobApplicationUi)
            implementation(projects.featuresTrackerEditsUi)

            // Lifecycle
            implementation(libs.bundles.androidx.lifecycle.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}
