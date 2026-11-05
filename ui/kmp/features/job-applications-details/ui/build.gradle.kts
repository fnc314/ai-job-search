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
            implementation(projects.featuresJobApplicationsDetailsDomain)
            implementation(projects.featuresJobApplicationsDetailsData)
            implementation(projects.coreWidgets)
            implementation(libs.bundles.androidx.lifecycle.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
