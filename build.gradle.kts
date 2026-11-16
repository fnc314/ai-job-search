plugins {
    alias(libs.plugins.dokka)
}

// Root build file for ai-job-search composite monorepo

dependencies {
    // Records Subsystem
    dokka(libs.records.parser)
    dokka(libs.records.proto)

    // Tools & Project Plugins
    dokka(libs.project.plugins)

    // Backend Subsystem
    dokka("dev.fnc314.tools.jobtracker.backend:service:1.0.0")
    dokka("dev.fnc314.tools.jobtracker.backend:job-post-ranking:1.0.0")
    dokka("dev.fnc314.tools.jobtracker.backend:tracker-conventions:1.0.0")
    dokka("dev.fnc314.tools.jobtracker.backend:email-classification:1.0.0")
    dokka("dev.fnc314.tools.jobtracker.backend:company-metadata-enrichment:1.0.0")
    dokka("dev.fnc314.tools.jobtracker.backend:event-chain:1.0.0")

    // Frontend Subsystem
    dokka("dev.fnc314.aijobtracker.frontend:apps-core:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:apps-cli:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:apps-terminal:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:apps-desktop:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:apps-android:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:apps-web:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:core-artifacts:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:core-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:core-design-system:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:core-logging:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:core-navigation:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:core-network:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:core-widgets:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-add-job-application-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-add-job-application-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-add-job-application-ui:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-artifact-previewer-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-artifact-previewer-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-artifact-previewer-ui:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-companies-list-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-companies-list-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-companies-list-ui:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-company-details-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-company-details-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-company-details-ui:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-application-details-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-application-details-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-application-details-ui:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-applications-list-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-applications-list-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-job-applications-list-ui:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-settings-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-settings-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-settings-ui:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-sign-in-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-sign-in-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-sign-in-ui:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-tracker-edits-data:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-tracker-edits-domain:1.0.0")
    dokka("dev.fnc314.aijobtracker.frontend:features-tracker-edits-ui:1.0.0")

    // Internal Subsystem
    dokka("dev.fnc314.tools.jobtracker.internal:apps-terminal-roadmap:1.0.0")
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
