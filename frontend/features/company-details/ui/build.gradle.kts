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
            implementation(projects.coreData)
            implementation(projects.coreNavigation)
            implementation(projects.featuresCompanyDetailsDomain)
            implementation(projects.featuresCompanyDetailsData)
            implementation(projects.coreWidgets)
            implementation(libs.bundles.androidx.lifecycle.compose)
            implementation(libs.bundles.coil)
            implementation(libs.compose.icons.font.awesome.brands)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
