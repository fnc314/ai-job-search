apply(from = "../gradle/tools/settings-plugins/subsystem.settings.gradle.kts")

rootProject.name = "ai-job-search_backend"

include(":service")
include(":job-post-ranking")
include(":tracker-conventions")
include(":email-classification")
include(":company-metadata-enrichment")
include(":event-chain")
includeBuild("../records")
