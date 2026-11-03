rootProject.name = "FNC314JobSearchTracker"

pluginManagement {
    includeBuild("./gradle/tools")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":desktopApp")
include(":shared")
include(":webApp")