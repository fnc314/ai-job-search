plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.dependencies.di)
    alias(libs.plugins.local.dependencies.datasource)
    alias(libs.plugins.local.dependencies.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreData)
            implementation(projects.coreNetwork)
            implementation(projects.featuresAiApplyDomain)
            implementation(libs.bundles.ktor.client)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
