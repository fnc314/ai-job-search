apply(from = "../gradle/conventions/subsystem.settings.gradle.kts")

rootProject.name = "records"

include(":records-proto")
project(":records-proto").projectDir = file("proto")
include(":records-parser")
project(":records-parser").projectDir = file("parser")
