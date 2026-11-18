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
        iosMain.dependencies {
            implementation(libs.kmp.inspector)
        }
        jvmMain.dependencies {
            implementation(libs.kmp.inspector)
        }
    }
}

dependencies {
    androidMainImplementation(libs.kmp.inspector)
}
