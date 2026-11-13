apply(from = "../gradle/tools/settings-plugins/subsystem.settings.gradle.kts")

rootProject.name = "backend"

include(":service")
project(":service").projectDir = file("service")

include(":job-post-ranking")
project(":job-post-ranking").projectDir = file("job-post-ranking")

include(":tracker-conventions")
project(":tracker-conventions").projectDir = file("tracker-conventions")

include(":email-classification")
project(":email-classification").projectDir = file("email-classification")

include(":company-metadata-enrichment")
project(":company-metadata-enrichment").projectDir = file("company-metadata-enrichment")

include(":event-chain")
project(":event-chain").projectDir = file("event-chain")

includeBuild("../records")
