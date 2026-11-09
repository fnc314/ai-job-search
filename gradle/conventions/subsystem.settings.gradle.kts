dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("${rootDir}/../gradle/libs.versions.toml"))
        }
    }
}
