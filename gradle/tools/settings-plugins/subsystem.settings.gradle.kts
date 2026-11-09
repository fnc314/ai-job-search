pluginManagement {
    val rootRepoDir = System.getenv("WORKSPACE_ROOT")?.let { java.io.File(it) }?.takeIf { it.exists() }
        ?: System.getenv("MISE_PROJECT_ROOT")?.let { java.io.File(it) }?.takeIf { it.exists() }
        ?: if (settings.layout.settingsDirectory.file("gradle/libs.versions.toml").asFile.exists()) {
            settings.settingsDir
        } else {
            settings.settingsDir.parentFile
        }
    includeBuild("${rootRepoDir}/gradle/tools/project-plugins")
}

dependencyResolutionManagement {
    val rootRepoDir = System.getenv("WORKSPACE_ROOT")?.let { java.io.File(it) }?.takeIf { it.exists() }
        ?: System.getenv("MISE_PROJECT_ROOT")?.let { java.io.File(it) }?.takeIf { it.exists() }
        ?: if (settings.layout.settingsDirectory.file("gradle/libs.versions.toml").asFile.exists()) {
            settings.settingsDir
        } else {
            settings.settingsDir.parentFile
        }
    versionCatalogs {
        if (findByName("libs") == null) {
            create("libs") {
                from(files("${rootRepoDir}/gradle/libs.versions.toml"))
            }
        }
    }
}
