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
}

rootProject.name = "ai-job-search"

includeBuild("backend")
includeBuild("frontend")
includeBuild("gradle/tools/project-plugins")
includeBuild("internal")
includeBuild("records")
includeBuild("tests")
