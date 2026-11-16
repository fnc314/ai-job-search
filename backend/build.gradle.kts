plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.local.dokka) apply false
}

subprojects {
    group = "dev.fnc314.tools.jobtracker.backend"
    version = "1.0.0"

    apply(plugin = "dev.fnc314.aijobtracker.dokka")
}

// Forwarding tasks for composite builds and root task orchestration
tasks.register("run") {
    group = "application"
    description = "Runs the main backend service application"
    dependsOn(":service:run")
}

tasks.register("installDist") {
    group = "distribution"
    description = "Installs the backend service distribution"
    dependsOn(":service:installDist")
}

tasks.register("jar") {
    group = "build"
    description = "Assembles the backend service JAR"
    dependsOn(":service:jar")
}

