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
    implementation(local.records.proto)
    implementation(local.records.parser)
    implementation(projects.coreData)
    implementation(libs.clikt)
    implementation(libs.mordant)
    implementation(libs.firebase.admin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    // backend modules published by composite build
    implementation(local.backend.job.post.ranking)
    implementation(local.backend.email.classification)
    implementation(local.backend.service)
    implementation(local.backend.ai.client)
}
