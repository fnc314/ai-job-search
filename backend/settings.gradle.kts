apply(from = "../gradle/tools/settings-plugins/subsystem.settings.gradle.kts")

rootProject.name = "backend"

include(":service")
project(":service").projectDir = file("service")

include(":job-post-ranking")
project(":job-post-ranking").projectDir = file("job-post-ranking")

includeBuild("../records")
