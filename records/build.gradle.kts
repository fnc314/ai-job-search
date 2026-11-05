plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.detekt)
}

val detektVersion = libs.versions.detekt.get()

subprojects {
    group = "dev.fnc314.tools.jobsearch.records"
    version = "1.0.0"

    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        toolVersion = detektVersion
        config.setFrom(
            rootProject.fileTree("../config/detekt") {
                include("detekt-*.yml")
                exclude("detekt-compose.yml")
            }
        )
        buildUponDefaultConfig = false
        allRules = false
        parallel = true
    }
}
