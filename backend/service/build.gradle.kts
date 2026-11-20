plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

application {
    mainClass.set("dev.fnc314.aijobtracker.backend.MainKt")
}

dependencies {
    implementation(projects.aiClient)
    implementation(projects.jobPostRanking)
    implementation(projects.jobPostScraping)
    implementation(projects.artifactCompilation)
    implementation(local.records.proto)
    implementation(local.records.parser)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.firebase.admin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mockk)
}

