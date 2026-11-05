plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.compose)
    alias(libs.plugins.local.dependencies.compose)
    alias(libs.plugins.local.dependencies.koin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreData)
            implementation(projects.coreDesignSystem)
            implementation(projects.coreWidgets)
            implementation(projects.featuresSettingsDomain)
            implementation(projects.featuresSettingsData)
            implementation(projects.featuresSignInDomain)
            implementation(projects.featuresSignInUi)
            implementation(libs.bundles.androidx.lifecycle.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
