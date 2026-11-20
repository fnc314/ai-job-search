plugins {
  alias(libs.plugins.detekt) apply false
  // this is necessary to avoid the plugins to be loaded multiple times
  // in each subproject's classloader
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidMultiplatformLibrary) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.kotlinSerialization) apply false
  alias(libs.plugins.koin.compiler) apply false
  alias(libs.plugins.google.play.services) apply false
  alias(libs.plugins.local.multiplatform.library) apply false
  alias(libs.plugins.local.kotlin.multiplatform) apply false
  alias(libs.plugins.local.android.application) apply false
  alias(libs.plugins.local.android.kmp.library) apply false
  alias(libs.plugins.local.compose) apply false
  alias(libs.plugins.local.dependencies.compose) apply false
  alias(libs.plugins.local.dependencies.serialization) apply false
  alias(libs.plugins.local.dependencies.di) apply false
  alias(libs.plugins.local.dependencies.koin) apply false
}


val frontendVersion = local.versions.version.get()

subprojects {
  group = "dev.fnc314.aijobtracker.frontend"
  version = frontendVersion

  apply(plugin = "dev.fnc314.aijobtracker.dokka")
}
