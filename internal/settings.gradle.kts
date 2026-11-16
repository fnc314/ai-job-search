apply(from = "../gradle/tools/settings-plugins/subsystem.settings.gradle.kts")

rootProject.name = "ai-job-tracker_internal"

include(":apps-terminal-roadmap")
project(":apps-terminal-roadmap").projectDir = file("apps/terminal/roadmap")
