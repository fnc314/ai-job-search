import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.dokka)
}

kotlin {
  jvmToolchain(libs.versions.java.toolchain.get().toInt())
  explicitApiWarning()
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.get())
    val kotlinVer = libs.versions.kotlin.asProvider().get().substringBeforeLast(".")
    apiVersion = KotlinVersion.fromVersion(kotlinVer)
    languageVersion = KotlinVersion.fromVersion(kotlinVer)
  }
}

dependencies {
  implementation(projects.recordsProto)
  implementation(libs.protobuf.kotlin)

  testImplementation(kotlin("test"))
  testImplementation(libs.konsist)
}

