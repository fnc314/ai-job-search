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
    implementation(project(":core-data"))
    implementation(libs.clikt)
    implementation(libs.mordant)
    implementation(libs.firebase.admin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
}
