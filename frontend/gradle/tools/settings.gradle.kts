rootProject.name = "gradle-tools"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../gradle/libs.versions.toml"))
    }
  }
}

include("project-plugins")
