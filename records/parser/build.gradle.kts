plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":records-proto"))
    implementation(libs.protobuf.kotlin)
}

