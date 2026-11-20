plugins {
    alias(libs.plugins.local.kotlin.multiplatform)
    alias(libs.plugins.local.android.kmp.library)
    alias(libs.plugins.local.dependencies.di)
    alias(libs.plugins.local.dependencies.datasource)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreData)
            implementation(projects.coreNetwork)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

dependencies {
    androidMainImplementation(libs.firebase.auth)
    androidMainImplementation(libs.androidx.credentials)
    androidMainImplementation(libs.androidx.credentials.play.services.auth)
    androidMainImplementation(libs.google.id)
    androidMainImplementation(libs.androidx.activity.compose)
}
