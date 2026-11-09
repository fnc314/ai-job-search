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
      implementation(libs.records.proto)
      implementation(libs.records.parser)
      implementation(libs.kmp.inspector)
      implementation(libs.kmp.inspector.room)
    }
    androidMain.dependencies {
      implementation(libs.bundles.ktor.client.android)
      implementation(libs.kotlinx.coroutinesPlayServices)
      implementation(libs.kmp.inspector)
      implementation(libs.kmp.inspector.room)
    }
    iosMain.dependencies {
      implementation(libs.bundles.ktor.client.ios)
      implementation(libs.kmp.inspector)
      implementation(libs.kmp.inspector.room)
    }
    jsMain.dependencies {
      implementation(libs.bundles.ktor.client.js)
    }
  }
}
