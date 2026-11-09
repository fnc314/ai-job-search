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
    implementation(libs.clikt)
    implementation(libs.mordant)
}
