plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.local.multiplatform.library) apply false
    alias(libs.plugins.local.kotlin.multiplatform) apply false
    alias(libs.plugins.local.android.kmp.library) apply false
    alias(libs.plugins.local.compose) apply false
    alias(libs.plugins.local.dependencies.compose) apply false
    alias(libs.plugins.local.dependencies.serialization) apply false
    alias(libs.plugins.local.dependencies.koin) apply false
}