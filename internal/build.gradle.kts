plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.local.dokka) apply false
}

subprojects {
    group = "dev.fnc314.aijobtracker.internal"
    version = "1.0.0"

    apply(plugin = "dev.fnc314.aijobtracker.dokka")
}
