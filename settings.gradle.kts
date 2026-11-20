@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
  versionCatalogs {
    create("local") {
      from(files("gradle/local.versions.toml"))
    }
  }
}

rootProject.name = "ai-job-tracker"

includeBuild("backend")
includeBuild("frontend")
includeBuild("gradle/tools/project-plugins")
includeBuild("internal")
includeBuild("records")
includeBuild("tests")
