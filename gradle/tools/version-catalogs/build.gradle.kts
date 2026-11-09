plugins {
    `version-catalog`
    `maven-publish`
}

group = "dev.fnc314.tools.jobtracker.gradle.tools"
version = "1.0.0"

catalog {
    versionCatalog {
        from(files("src/main/resources/libs.versions.toml"))
    }
}

publishing {
    publications {
        create<MavenPublication>("versionCatalog") {
            from(components["versionCatalog"])
        }
    }
}
