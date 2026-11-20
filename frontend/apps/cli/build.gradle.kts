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
    val backendVersion = libs.versions.dev.fnc314.aijobtracker.backend.get()
    implementation("dev.fnc314.aijobtracker.backend:job-post-ranking:$backendVersion")
    implementation("dev.fnc314.aijobtracker.backend:email-classification:$backendVersion")
    implementation("dev.fnc314.aijobtracker.backend:service:$backendVersion")
    implementation("dev.fnc314.aijobtracker.backend:ai-client:$backendVersion")
}
