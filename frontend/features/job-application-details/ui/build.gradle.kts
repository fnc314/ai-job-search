plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.compose)
    alias(libs.plugins.local.dependencies.compose)
    alias(libs.plugins.local.dependencies.di)
    alias(libs.plugins.local.dependencies.navigation)
    alias(libs.plugins.local.dependencies.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreArtifacts)
            implementation(projects.coreData)
            implementation(projects.coreNavigation)
            implementation(projects.featuresArtifactPreviewerUi)
            implementation(projects.featuresJobApplicationDetailsDomain)
            implementation(projects.featuresJobApplicationDetailsData)
            implementation(projects.coreWidgets)
            implementation(libs.bundles.androidx.lifecycle.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.markdown.renderer.m3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
