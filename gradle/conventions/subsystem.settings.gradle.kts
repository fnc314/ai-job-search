val rootRepoDir = if (file("${rootDir}/gradle/libs.versions.toml").exists()) {
    rootDir
} else {
    file("${rootDir}/..")
}

dependencyResolutionManagement {
    versionCatalogs {
        if (findByName("libs") == null) {
            create("libs") {
                from(files("${rootRepoDir}/gradle/libs.versions.toml"))
            }
        }
    }
}
