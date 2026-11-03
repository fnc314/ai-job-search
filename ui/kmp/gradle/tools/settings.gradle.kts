rootProject.name = "gradle-tools"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("./../libs.versions.toml"))
    }
  }
}

include("project-plugins")