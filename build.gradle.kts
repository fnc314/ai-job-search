// Root build file for ai-job-search composite monorepo

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
