plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
}

kotlin {
    jvmToolchain(17)
    explicitApiWarning()
}

dependencies {
    implementation(project(":records-proto"))
    implementation(libs.protobuf.kotlin)
}

