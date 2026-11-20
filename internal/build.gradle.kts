plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.local.dokka) apply false
}

val internalVersion = libs.versions.dev.fnc314.aijobtracker.asProvider().get()

subprojects {
    group = "dev.fnc314.aijobtracker.internal"
    version = internalVersion

    apply(plugin = "dev.fnc314.aijobtracker.dokka")
}
