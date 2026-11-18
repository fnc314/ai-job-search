plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    explicitApi()
}

application {
    mainClass.set("dev.fnc314.aijobtracker.frontend.cli.MainKt")
}

dependencies {
    implementation(libs.records.proto)
    implementation(libs.records.parser)
    implementation(projects.coreData)
    implementation(libs.clikt)
    implementation(libs.mordant)
    implementation(libs.firebase.admin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    // job-post-ranking is published by the backend composite build
    implementation("dev.fnc314.tools.jobtracker.backend:job-post-ranking:1.0.0")
    implementation("dev.fnc314.tools.jobtracker.backend:email-classification:1.0.0")
    implementation("dev.fnc314.tools.jobtracker.backend:service:1.0.0")
}
