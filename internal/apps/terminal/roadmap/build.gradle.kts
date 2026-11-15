plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    explicitApi()
}

application {
    mainClass.set("dev.fnc314.tools.jobtracker.internal.terminal.roadmap.MainKt")
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.mordant)
    implementation(libs.mordant.jna)

    // Exclude FFM provider which requires JVM 22+ and fails at runtime on getStdinTermios()
    // JNA provider is also excluded at runtime via stty fallback for terminals that don't support tcgetattr
    configurations.all {
        exclude(group = "com.github.ajalt.mordant", module = "mordant-jvm-ffm-jvm")
    }

    testImplementation(libs.kotlin.test)
}
