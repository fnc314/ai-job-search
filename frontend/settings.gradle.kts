import dev.fnc314.gradle.plugins.settings.projectcollectionsgradlesettingsplugin.ProjectNameMapper

apply(from = "../gradle/tools/settings-plugins/subsystem.settings.gradle.kts")

rootProject.name = "FNC314JobSearchTracker"

pluginManagement {
    plugins {
        id("dev.fnc314.gradle.plugins.settings.project-collections-gradle-settings-plugin") version("5.1.1")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.fnc314.gradle.plugins.settings.project-collections-gradle-settings-plugin") version "5.1.1"
}

projectCollections {
    registerProjectCollection("apps")
    registerProjectCollection("core")
    registerProjectCollection("features", depth = 2)
    projectNameTransform = ProjectNameMapper { name, _ ->
        name.removePrefix(":")
            .replace(":", "-")
            .prependIndent(":")
    }
}

includeBuild("../records")
