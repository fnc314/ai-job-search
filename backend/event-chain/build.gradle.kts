import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(libs.versions.java.toolchain.get().toInt())
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.get())
        val kotlinVer = libs.versions.kotlin.asProvider().get().substringBeforeLast(".")
        apiVersion = KotlinVersion.fromVersion(kotlinVer)
        languageVersion = KotlinVersion.fromVersion(kotlinVer)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.recordsProto)

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
            groupId = "dev.fnc314.tools.jobtracker.backend"
            artifactId = "event-chain"
            version = "1.0.0"
        }
    }
}
