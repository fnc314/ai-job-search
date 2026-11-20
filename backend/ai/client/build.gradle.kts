import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.toolchain.get())
        val kotlinVer = libs.versions.kotlin.asProvider().get().substringBeforeLast(".")
        apiVersion = KotlinVersion.fromVersion(kotlinVer)
        languageVersion = KotlinVersion.fromVersion(kotlinVer)
    }
}

dependencies {
    implementation(projects.jobPostRanking)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "dev.fnc314.aijobtracker.backend"
            artifactId = "ai-client"
            version = "1.0.0"
        }
    }
}
