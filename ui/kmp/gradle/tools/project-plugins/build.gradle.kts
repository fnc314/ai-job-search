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
  gradleApi()
  implementation(libs.android.gradle.plugin)
}

gradlePlugin {
  plugins {
    create("multiplatform-library") {
      id = libs.plugins.local.multiplatform.library.map { it.pluginId }.get()
      description = "Applies Android Kotlin Multiplatform Library Plugin"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.MultiplatformLibrary"
      tags.addAll(
        setOf(
          "Android", "Kotlin", "Androd Library", "Kotlin Multiplatform Mobile"
        )
      )
      displayName = "Multiplatform Library"
    }
  }
}