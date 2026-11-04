plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.protobuf) apply false
}

subprojects {
    group = "dev.fnc314.tools.jobsearch.records"
    version = "1.0.0"
}
