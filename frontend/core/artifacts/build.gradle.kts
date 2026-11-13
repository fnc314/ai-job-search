plugins {
  alias(libs.plugins.local.kotlin.multiplatform)
  alias(libs.plugins.local.android.kmp.library)
  alias(libs.plugins.local.compose)
  alias(libs.plugins.local.dependencies.compose)
  alias(libs.plugins.local.dependencies.serialization)
  alias(libs.plugins.local.dependencies.di)
  alias(libs.plugins.local.dependencies.datasource)
  alias(libs.plugins.local.dependencies.firebase)
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(projects.coreData)
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
    }
    androidMain.dependencies {
      implementation(libs.bundles.ktor.client.android)
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
