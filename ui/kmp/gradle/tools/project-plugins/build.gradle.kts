plugins {
  alias(libs.plugins.kotlinJvm)
  `java-gradle-plugin`
}

kotlin {
  jvmToolchain(
    libs.versions.jvm.get().toInt()
  )
}

dependencies {
  implementation(libs.android.gradle.plugin)
}