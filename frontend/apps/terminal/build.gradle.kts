plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

application {
    mainClass.set("dev.fnc314.tools.jobtracker.terminal.MainKt")
}

dependencies {
    implementation(libs.records.proto)
    implementation(libs.records.parser)
    implementation(projects.coreData)
    implementation(libs.clikt)
    implementation(libs.mordant)
    implementation(libs.mordant.jna)

    // Exclude FFM provider which requires JVM 22+ and fails at runtime on getStdinTermios()
    // JNA provider is also excluded at runtime via stty fallback in Main.kt for terminals
    // that don't support tcgetattr (e.g., Ghostty)
    configurations.all {
        exclude(group = "com.github.ajalt.mordant", module = "mordant-jvm-ffm-jvm")
    }
    implementation(libs.firebase.admin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
}
