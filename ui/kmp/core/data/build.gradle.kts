plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.compose)
    alias(libs.plugins.local.dependencies.compose)
    alias(libs.plugins.local.dependencies.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreNetwork)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.bundles.ktor.client)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.bundles.ktor.client.jvm)
            implementation(libs.bundles.firebase.jvm)
            implementation(libs.records.proto)
            implementation(libs.records.parser)
        }
        androidMain.dependencies {
            implementation(libs.bundles.ktor.client.android)
            implementation(libs.bundles.firebase.android)
            implementation(libs.kotlinx.coroutinesPlayServices)
        }
        iosMain.dependencies {
            implementation(libs.bundles.ktor.client.ios)
        }
        jsMain.dependencies {
            implementation(libs.bundles.ktor.client.js)
        }
    }
}
