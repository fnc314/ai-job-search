rootProject.name = "records"

include(":records-proto")
project(":records-proto").projectDir = file("proto")
include(":records-parser")
project(":records-parser").projectDir = file("parser")

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
