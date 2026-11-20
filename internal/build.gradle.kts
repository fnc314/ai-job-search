plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.local.dokka) apply false
}

val internalVersion = local.versions.version.get()

subprojects {
    group = "dev.fnc314.aijobtracker.internal"
    version = internalVersion

    apply(plugin = "dev.fnc314.aijobtracker.dokka")
}
