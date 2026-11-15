rootProject.name = "ai-job-search_gradle-tools_project-plugins"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../libs.versions.toml"))
        }
    }
}
