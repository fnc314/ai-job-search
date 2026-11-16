apply(from = "../gradle/tools/settings-plugins/subsystem.settings.gradle.kts")

rootProject.name = "ai-job-tracker_backend"

include(":service")
include(":job-post-ranking")
include(":job-post-scraping")
include(":artifact-compilation")
include(":tracker-conventions")
include(":email-classification")
include(":company-metadata-enrichment")
include(":event-chain")
includeBuild("../records")
