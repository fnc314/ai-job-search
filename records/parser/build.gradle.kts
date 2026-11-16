import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.local.dokka)
}

kotlin {
  jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
  explicitApi()
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.toolchain.get())
    val kotlinVer = libs.versions.kotlin.asProvider().get().substringBeforeLast(".")
    apiVersion = KotlinVersion.fromVersion(kotlinVer)
    languageVersion = KotlinVersion.fromVersion(kotlinVer)
  }
}

dependencies {
  implementation(project(":records-proto"))
  implementation(libs.protobuf.kotlin)
  implementation(libs.kotlinx.serialization.json)

  testImplementation(kotlin("test"))
}

