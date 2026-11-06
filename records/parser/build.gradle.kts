import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
}

kotlin {
  jvmToolchain(libs.versions.java.toolchains.get().toInt())
  explicitApiWarning()
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.get())
    apiVersion = KotlinVersion.fromVersion(
      libs.versions.kotlin.get().substringBeforeLast(".")
    )
    languageVersion = KotlinVersion.fromVersion(
      libs.versions.kotlin.get().substringBeforeLast(".")
    )
  }
}

dependencies {
  implementation(projects.recordsProto)
  implementation(libs.protobuf.kotlin)

  testImplementation(kotlin("test"))
  testImplementation(libs.konsist)
}

