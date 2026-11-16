plugins {
    alias(libs.plugins.kotlinJvm)
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
    implementation(project(":job-post-ranking"))
    implementation(project(":job-post-scraping"))
    implementation(project(":artifact-compilation"))
    implementation(libs.records.proto)
    implementation(libs.records.parser)
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

