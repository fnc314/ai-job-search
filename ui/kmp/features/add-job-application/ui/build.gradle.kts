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
            implementation(projects.coreWidgets)
            implementation(projects.featuresAddJobApplicationDomain)
            implementation(projects.featuresAddJobApplicationData)
            implementation(libs.bundles.androidx.lifecycle.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
