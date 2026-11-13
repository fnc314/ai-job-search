plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.dependencies.koin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreArtifacts)
            implementation(projects.coreData)
            implementation(projects.featuresArtifactPreviewerData)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
    }
}
