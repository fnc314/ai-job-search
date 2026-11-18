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
        iosMain.dependencies {
            implementation(libs.bundles.ktor.client.ios)
            implementation(libs.kmp.inspector)
            implementation(libs.kmp.inspector.ktor)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.ktor.client.jvm)
            implementation(libs.kmp.inspector)
            implementation(libs.kmp.inspector.ktor)
        }
        jsMain.dependencies {
            implementation(libs.bundles.ktor.client.js)
        }
    }
}

dependencies {
    androidMainImplementation(libs.bundles.ktor.client.android)
    androidMainImplementation(libs.kmp.inspector)
    androidMainImplementation(libs.kmp.inspector.ktor)
}
