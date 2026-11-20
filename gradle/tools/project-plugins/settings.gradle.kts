rootProject.name = "ai-job-tracker_gradle-tools_project-plugins"

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
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../libs.versions.toml"))
        }
        create("local") {
            from(files("../../local.versions.toml"))
        }
    }
}
