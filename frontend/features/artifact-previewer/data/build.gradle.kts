plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.dependencies.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreArtifacts)
            implementation(projects.coreData)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
