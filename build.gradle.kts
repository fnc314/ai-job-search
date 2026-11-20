plugins {
    alias(libs.plugins.dokka)
}

// Root build file for ai-job-tracker composite monorepo

dependencies {
    val backendVersion = libs.versions.dev.fnc314.aijobtracker.backend.get()
    val frontendVersion = libs.versions.dev.fnc314.aijobtracker.asProvider().get()
    val internalVersion = libs.versions.dev.fnc314.aijobtracker.asProvider().get()

    // Records Subsystem
    dokka(libs.bundles.dev.fnc314.aijobtracker.records)

    // Tools & Project Plugins
    dokka(libs.project.plugins)

    // Backend Subsystem
    dokka("dev.fnc314.aijobtracker.backend:ai-client:$backendVersion")
    dokka("dev.fnc314.aijobtracker.backend:service:$backendVersion")
    dokka("dev.fnc314.aijobtracker.backend:job-post-ranking:$backendVersion")
    dokka("dev.fnc314.aijobtracker.backend:tracker-conventions:$backendVersion")
    dokka("dev.fnc314.aijobtracker.backend:email-classification:$backendVersion")
    dokka("dev.fnc314.aijobtracker.backend:company-metadata-enrichment:$backendVersion")
    dokka("dev.fnc314.aijobtracker.backend:event-chain:$backendVersion")

    // Frontend Subsystem
    dokka("dev.fnc314.aijobtracker.frontend:apps-core:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:apps-cli:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:apps-terminal:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:apps-desktop:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:apps-android:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:apps-web:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:core-artifacts:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:core-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:core-design-system:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:core-logging:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:core-navigation:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:core-network:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:core-widgets:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-add-job-application-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-add-job-application-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-add-job-application-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-ai-apply-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-ai-apply-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-ai-apply-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-artifact-previewer-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-artifact-previewer-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-artifact-previewer-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-companies-list-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-companies-list-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-companies-list-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-company-details-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-company-details-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-company-details-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-application-details-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-application-details-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-application-details-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-applications-list-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-applications-list-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-applications-list-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-settings-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-settings-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-settings-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-sign-in-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-sign-in-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-sign-in-ui:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-tracker-edits-data:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-tracker-edits-domain:$frontendVersion")
    dokka("dev.fnc314.aijobtracker.frontend:features-tracker-edits-ui:$frontendVersion")

    // Internal Subsystem
    dokka("dev.fnc314.aijobtracker.internal:apps-terminal-roadmap:$internalVersion")
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
