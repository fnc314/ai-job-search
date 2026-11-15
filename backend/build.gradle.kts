plugins {
    alias(libs.plugins.kotlinJvm) apply false
}

subprojects {
    group = "dev.fnc314.tools.jobtracker.backend"
    version = "1.0.0"
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

