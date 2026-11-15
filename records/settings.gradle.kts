apply(from = "../gradle/tools/settings-plugins/subsystem.settings.gradle.kts")

rootProject.name = "ai-job-search_records"

include(":records-proto")
project(":records-proto").projectDir = file("proto")
include(":records-parser")
project(":records-parser").projectDir = file("parser")
