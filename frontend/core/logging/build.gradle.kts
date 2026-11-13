plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.dependencies.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kermit)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.kmp.inspector)
        }
        iosMain.dependencies {
            implementation(libs.kmp.inspector)
        }
        jvmMain.dependencies {
            implementation(libs.kmp.inspector)
        }
    }
}
