plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":records-proto"))
    implementation(libs.protobuf.kotlin)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
    repositories {
        maven {
            name = "local"
            url = file("${rootProject.rootDir}/../.m2").toURI()
        }
    }
}
