plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.compose)
    alias(libs.plugins.local.dependencies.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.bundles.compose.ui)
            api(libs.materialKolor.material3)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
    }
}
