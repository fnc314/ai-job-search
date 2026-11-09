import java.time.Instant

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.dokka)
  `java-gradle-plugin`
}

group = "dev.fnc314.tools.jobtracker.tools"
version = "1.0.0"

dokka {
  dokkaPublications.html {
    moduleName.set(":tools:project-plugins")
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
  }
}

kotlin {
  explicitApi()
  jvmToolchain(
    libs.versions.javaToolchain.get().toInt()
  )
}

dependencies {
  gradleApi()
  implementation(libs.android.gradle.plugin)
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.compose.gradle.plugin)
  implementation(libs.ksp.gradle.plugin)
  implementation(libs.dokka.gradle.plugin)
  implementation(libs.detekt.gradle.plugin)
  implementation(libs.koin.compiler.gradle.plugin)
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
    create("android-application") {
      id = libs.plugins.local.android.application.map { it.pluginId }.get()
      description = "Applies Android Application plugin and SDK conventions"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.functional.AndroidApplicationConventionPlugin"
      displayName = "Android KMP Library Convention"
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
    create("dependencies-firebase") {
      id = libs.plugins.local.dependencies.firebase.map { it.pluginId }.get()
      description = "Configures Firebase dependencies"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.FirebaseDependenciesConventionPlugin"
      displayName = "Firebase Dependencies Convention"
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
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.KoinDependenciesConventionPlugin"
      displayName = "DI Dependencies Convention"
    }
    create("dependencies-koin") {
      id = libs.plugins.local.dependencies.koin.map { it.pluginId }.get()
      description = "Applies DI dependency bundles (legacy alias)"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.KoinDependenciesConventionPlugin"
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
    create("dokka") {
      id = libs.plugins.local.dokka.map { it.pluginId }.get()
      description = "Applies Dokka documentation generation conventions"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.DokkaConventionPlugin"
      displayName = "Dokka Documentation Convention"
    }
    create("detekt") {
      id = libs.plugins.local.detekt.map { it.pluginId }.get()
      description = "Applies Detekt static analysis and formatting conventions"
      implementationClass = "dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config.DetektConventionPlugin"
      displayName = "Detekt Static Analysis Convention"
    }
  }
}