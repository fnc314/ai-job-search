plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.dependencies.serialization)
    alias(libs.plugins.local.dependencies.di)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.bundles.ktor.client)
        }
        androidMain.dependencies {
            implementation(libs.bundles.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.bundles.ktor.client.ios)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.ktor.client.jvm)
        }
        jsMain.dependencies {
            implementation(libs.bundles.ktor.client.js)
        }
    }
}
