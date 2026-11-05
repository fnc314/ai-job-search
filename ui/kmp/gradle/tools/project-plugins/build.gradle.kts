plugins {
  alias(libs.plugins.kotlinJvm)
  `java-gradle-plugin`
}

kotlin {
  explicitApi()
  jvmToolchain(
    libs.versions.jvm.get().toInt()
  )
}

dependencies {
  gradleApi()
  implementation(libs.android.gradle.plugin)
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.compose.gradle.plugin)
}

gradlePlugin {
  plugins {
    create("multiplatform-library") {
      id = libs.plugins.local.multiplatform.library.map { it.pluginId }.get()
      description = "Applies Android Kotlin Multiplatform Library Plugin"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.MultiplatformLibrary"
      displayName = "Multiplatform Library"
    }
    create("kotlin-multiplatform") {
      id = libs.plugins.local.kotlin.multiplatform.map { it.pluginId }.get()
      description = "Applies Kotlin Multiplatform toolchains and target conventions"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.functional.KotlinMultiplatformConventionPlugin"
      displayName = "Kotlin Multiplatform Convention"
    }
    create("android-kmp-library") {
      id = libs.plugins.local.android.kmp.library.map { it.pluginId }.get()
      description = "Applies Android KMP Library toolchain and SDK conventions"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.functional.AndroidKmpLibraryConventionPlugin"
      displayName = "Android KMP Library Convention"
    }
    create("compose") {
      id = libs.plugins.local.compose.map { it.pluginId }.get()
      description = "Applies Compose Multiplatform and Compose Compiler plugins"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.functional.ComposeConventionPlugin"
      displayName = "Compose Multiplatform Convention"
    }
    create("dependencies-compose") {
      id = libs.plugins.local.dependencies.compose.map { it.pluginId }.get()
      description = "Applies Compose UI and tooling dependency bundles"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.ComposeDependenciesConventionPlugin"
      displayName = "Compose Dependencies Convention"
    }
    create("dependencies-serialization") {
      id = libs.plugins.local.dependencies.serialization.map { it.pluginId }.get()
      description = "Applies Kotlin Serialization plugin and runtime library"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.SerializationDependenciesConventionPlugin"
      displayName = "Serialization Dependencies Convention"
    }
    create("dependencies-di") {
      id = libs.plugins.local.dependencies.di.map { it.pluginId }.get()
      description = "Applies DI dependency bundles"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.DIDependenciesConventionPlugin"
      displayName = "DI Dependencies Convention"
    }
    create("dependencies-koin") {
      id = libs.plugins.local.dependencies.koin.map { it.pluginId }.get()
      description = "Applies DI dependency bundles (legacy alias)"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.DIDependenciesConventionPlugin"
      displayName = "Koin Dependencies Convention"
    }
    create("dependencies-navigation") {
      id = libs.plugins.local.dependencies.navigation.map { it.pluginId }.get()
      description = "Applies Compose Navigation and Serialization dependencies"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.NavigationDependenciesConventionPlugin"
      displayName = "Navigation Dependencies Convention"
    }
    create("dependencies-datasource") {
      id = libs.plugins.local.dependencies.datasource.map { it.pluginId }.get()
      description = "Applies Room and SQLite data source dependency bundles across targets"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.DataSourceDependenciesConventionPlugin"
      displayName = "Data Source Dependencies Convention"
    }
  }
}