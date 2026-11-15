plugins {
    alias(libs.plugins.dokka)
}

// Root build file for ai-job-search composite monorepo

dependencies {
    dokka(libs.records.parser)
    dokka(libs.records.proto)
    dokka(libs.project.plugins)
}

dokka {
    dokkaPublications.html {
        moduleName.set("AI Job Search & Application OS Documentation")
        outputDirectory.set(layout.projectDirectory.dir("dokka"))
    }
}



tasks.register("buildAll") {
    group = "build"
    description = "Assembles all included builds across records, backend, and frontend"
    dependsOn(
        gradle.includedBuild("records").task(":records-proto:jar"),
        gradle.includedBuild("records").task(":records-parser:jar"),
        gradle.includedBuild("backend").task(":jar"),
        gradle.includedBuild("frontend").task(":apps-cli:installDist"),
        gradle.includedBuild("frontend").task(":apps-terminal:installDist"),
    )
}
