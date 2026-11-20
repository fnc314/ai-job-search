# Architecture Plan: Decouple Local Project Catalog into `local.versions.toml`

**Status:** Completed
**Date:** 2026-11-20
**Target:** `gradle/local.versions.toml`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `gradle/tools/settings-plugins/subsystem.settings.gradle.kts`, `build.gradle.kts`

---

## 1. Context & Motivation

Currently, `gradle/libs.versions.toml` mixes external third-party dependencies (Kotlin, AGP, Coroutines, Firebase, etc.) with local internal project coordinate definitions (`records`, `project-plugins`, `dev-fnc314-aijobtracker`). Furthermore, `build.gradle.kts` declares raw GAV strings for ~40 internal modules across the `backend`, `frontend`, and `internal` subsystems.

The user's goal:
1. Extract all `dev-fnc314-aijobtracker` project and subsystem declarations into a separate version catalog file: `gradle/local.versions.toml`.
2. Maintain a single `[versions]` declaration therein (`version = "2.7.2"`), strictly aligned with `gradle.properties` (`version=2.7.2`).
3. Define clean catalog bundles (`local.bundles.dev.fnc314.aijobtracker.backend`, `local.bundles.dev.fnc314.aijobtracker.frontend`, `local.bundles.dev.fnc314.aijobtracker.internal`, `local.bundles.dev.fnc314.aijobtracker.records`) to replace all hardcoded strings in root `build.gradle.kts` and inter-module dependencies.
4. Register the `local` version catalog across all composite included builds via `subsystem.settings.gradle.kts`, `settings.gradle.kts`, and `gradle/tools/project-plugins/settings.gradle.kts`.

---

## 2. Catalog Specifications

### `gradle/local.versions.toml`
```toml
[versions]
  version = "2.7.2"

[libraries]
  # Records Subsystem
  records-parser = { group = "dev.fnc314.aijobtracker.records", name = "records-parser", version.ref = "version" }
  records-proto = { group = "dev.fnc314.aijobtracker.records", name = "records-proto", version.ref = "version" }

  # Tools
  project-plugins = { module = "dev.fnc314.aijobtracker.tools:ai-job-tracker_gradle-tools_project-plugins", version.ref = "version" }

  # Backend Subsystem
  backend-ai-client = { module = "dev.fnc314.aijobtracker.backend:ai-client", version.ref = "version" }
  backend-service = { module = "dev.fnc314.aijobtracker.backend:service", version.ref = "version" }
  backend-job-post-ranking = { module = "dev.fnc314.aijobtracker.backend:job-post-ranking", version.ref = "version" }
  backend-tracker-conventions = { module = "dev.fnc314.aijobtracker.backend:tracker-conventions", version.ref = "version" }
  backend-email-classification = { module = "dev.fnc314.aijobtracker.backend:email-classification", version.ref = "version" }
  backend-company-metadata-enrichment = { module = "dev.fnc314.aijobtracker.backend:company-metadata-enrichment", version.ref = "version" }
  backend-event-chain = { module = "dev.fnc314.aijobtracker.backend:event-chain", version.ref = "version" }
  backend-job-post-scraping = { module = "dev.fnc314.aijobtracker.backend:job-post-scraping", version.ref = "version" }
  backend-artifact-compilation = { module = "dev.fnc314.aijobtracker.backend:artifact-compilation", version.ref = "version" }

  # Frontend Subsystem - Apps & Core
  frontend-apps-core = { module = "dev.fnc314.aijobtracker.frontend:apps-core", version.ref = "version" }
  frontend-apps-cli = { module = "dev.fnc314.aijobtracker.frontend:apps-cli", version.ref = "version" }
  frontend-apps-terminal = { module = "dev.fnc314.aijobtracker.frontend:apps-terminal", version.ref = "version" }
  frontend-apps-desktop = { module = "dev.fnc314.aijobtracker.frontend:apps-desktop", version.ref = "version" }
  frontend-apps-android = { module = "dev.fnc314.aijobtracker.frontend:apps-android", version.ref = "version" }
  frontend-apps-web = { module = "dev.fnc314.aijobtracker.frontend:apps-web", version.ref = "version" }
  frontend-core-artifacts = { module = "dev.fnc314.aijobtracker.frontend:core-artifacts", version.ref = "version" }
  frontend-core-data = { module = "dev.fnc314.aijobtracker.frontend:core-data", version.ref = "version" }
  frontend-core-design-system = { module = "dev.fnc314.aijobtracker.frontend:core-design-system", version.ref = "version" }
  frontend-core-logging = { module = "dev.fnc314.aijobtracker.frontend:core-logging", version.ref = "version" }
  frontend-core-navigation = { module = "dev.fnc314.aijobtracker.frontend:core-navigation", version.ref = "version" }
  frontend-core-network = { module = "dev.fnc314.aijobtracker.frontend:core-network", version.ref = "version" }
  frontend-core-widgets = { module = "dev.fnc314.aijobtracker.frontend:core-widgets", version.ref = "version" }

  # Frontend Subsystem - Features
  frontend-features-add-job-application-data = { module = "dev.fnc314.aijobtracker.frontend:features-add-job-application-data", version.ref = "version" }
  frontend-features-add-job-application-domain = { module = "dev.fnc314.aijobtracker.frontend:features-add-job-application-domain", version.ref = "version" }
  frontend-features-add-job-application-ui = { module = "dev.fnc314.aijobtracker.frontend:features-add-job-application-ui", version.ref = "version" }
  frontend-features-ai-apply-data = { module = "dev.fnc314.aijobtracker.frontend:features-ai-apply-data", version.ref = "version" }
  frontend-features-ai-apply-domain = { module = "dev.fnc314.aijobtracker.frontend:features-ai-apply-domain", version.ref = "version" }
  frontend-features-ai-apply-ui = { module = "dev.fnc314.aijobtracker.frontend:features-ai-apply-ui", version.ref = "version" }
  frontend-features-artifact-previewer-data = { module = "dev.fnc314.aijobtracker.frontend:features-artifact-previewer-data", version.ref = "version" }
  frontend-features-artifact-previewer-domain = { module = "dev.fnc314.aijobtracker.frontend:features-artifact-previewer-domain", version.ref = "version" }
  frontend-features-artifact-previewer-ui = { module = "dev.fnc314.aijobtracker.frontend:features-artifact-previewer-ui", version.ref = "version" }
  frontend-features-companies-list-data = { module = "dev.fnc314.aijobtracker.frontend:features-companies-list-data", version.ref = "version" }
  frontend-features-companies-list-domain = { module = "dev.fnc314.aijobtracker.frontend:features-companies-list-domain", version.ref = "version" }
  frontend-features-companies-list-ui = { module = "dev.fnc314.aijobtracker.frontend:features-companies-list-ui", version.ref = "version" }
  frontend-features-company-details-data = { module = "dev.fnc314.aijobtracker.frontend:features-company-details-data", version.ref = "version" }
  frontend-features-company-details-domain = { module = "dev.fnc314.aijobtracker.frontend:features-company-details-domain", version.ref = "version" }
  frontend-features-company-details-ui = { module = "dev.fnc314.aijobtracker.frontend:features-company-details-ui", version.ref = "version" }
  frontend-features-job-application-details-data = { module = "dev.fnc314.aijobtracker.frontend:features-job-application-details-data", version.ref = "version" }
  frontend-features-job-application-details-domain = { module = "dev.fnc314.aijobtracker.frontend:features-job-application-details-domain", version.ref = "version" }
  frontend-features-job-application-details-ui = { module = "dev.fnc314.aijobtracker.frontend:features-job-application-details-ui", version.ref = "version" }
  frontend-features-job-applications-list-data = { module = "dev.fnc314.aijobtracker.frontend:features-job-applications-list-data", version.ref = "version" }
  frontend-features-job-applications-list-domain = { module = "dev.fnc314.aijobtracker.frontend:features-job-applications-list-domain", version.ref = "version" }
  frontend-features-job-applications-list-ui = { module = "dev.fnc314.aijobtracker.frontend:features-job-applications-list-ui", version.ref = "version" }
  frontend-features-settings-data = { module = "dev.fnc314.aijobtracker.frontend:features-settings-data", version.ref = "version" }
  frontend-features-settings-domain = { module = "dev.fnc314.aijobtracker.frontend:features-settings-domain", version.ref = "version" }
  frontend-features-settings-ui = { module = "dev.fnc314.aijobtracker.frontend:features-settings-ui", version.ref = "version" }
  frontend-features-sign-in-data = { module = "dev.fnc314.aijobtracker.frontend:features-sign-in-data", version.ref = "version" }
  frontend-features-sign-in-domain = { module = "dev.fnc314.aijobtracker.frontend:features-sign-in-domain", version.ref = "version" }
  frontend-features-sign-in-ui = { module = "dev.fnc314.aijobtracker.frontend:features-sign-in-ui", version.ref = "version" }
  frontend-features-tracker-edits-data = { module = "dev.fnc314.aijobtracker.frontend:features-tracker-edits-data", version.ref = "version" }
  frontend-features-tracker-edits-domain = { module = "dev.fnc314.aijobtracker.frontend:features-tracker-edits-domain", version.ref = "version" }
  frontend-features-tracker-edits-ui = { module = "dev.fnc314.aijobtracker.frontend:features-tracker-edits-ui", version.ref = "version" }

  # Internal Subsystem
  internal-apps-terminal-roadmap = { module = "dev.fnc314.aijobtracker.internal:apps-terminal-roadmap", version.ref = "version" }

[bundles]
  records = [
    "records-parser",
    "records-proto",
  ]
  backend = [
    "backend-ai-client",
    "backend-service",
    "backend-job-post-ranking",
    "backend-tracker-conventions",
    "backend-email-classification",
    "backend-company-metadata-enrichment",
    "backend-event-chain",
  ]
  frontend = [
    "frontend-apps-core",
    "frontend-apps-cli",
    "frontend-apps-terminal",
    "frontend-apps-desktop",
    "frontend-apps-android",
    "frontend-apps-web",
    "frontend-core-artifacts",
    "frontend-core-data",
    "frontend-core-design-system",
    "frontend-core-logging",
    "frontend-core-navigation",
    "frontend-core-network",
    "frontend-core-widgets",
    "frontend-features-add-job-application-data",
    "frontend-features-add-job-application-domain",
    "frontend-features-add-job-application-ui",
    "frontend-features-ai-apply-data",
    "frontend-features-ai-apply-domain",
    "frontend-features-ai-apply-ui",
    "frontend-features-artifact-previewer-data",
    "frontend-features-artifact-previewer-domain",
    "frontend-features-artifact-previewer-ui",
    "frontend-features-companies-list-data",
    "frontend-features-companies-list-domain",
    "frontend-features-companies-list-ui",
    "frontend-features-company-details-data",
    "frontend-features-company-details-domain",
    "frontend-features-company-details-ui",
    "frontend-features-job-application-details-data",
    "frontend-features-job-application-details-domain",
    "frontend-features-job-application-details-ui",
    "frontend-features-job-applications-list-data",
    "frontend-features-job-applications-list-domain",
    "frontend-features-job-applications-list-ui",
    "frontend-features-settings-data",
    "frontend-features-settings-domain",
    "frontend-features-settings-ui",
    "frontend-features-sign-in-data",
    "frontend-features-sign-in-domain",
    "frontend-features-sign-in-ui",
    "frontend-features-tracker-edits-data",
    "frontend-features-tracker-edits-domain",
    "frontend-features-tracker-edits-ui",
  ]
  internal = [
    "internal-apps-terminal-roadmap",
  ]
```

---

## 3. Integration & Settings Configuration

In `gradle/tools/settings-plugins/subsystem.settings.gradle.kts`, root `settings.gradle.kts`, and `gradle/tools/project-plugins/settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        if (findByName("local") == null) {
            create("local") {
                from(files("${rootRepoDir}/gradle/local.versions.toml"))
            }
        }
    }
}
```

---

## 4. Subsystem Version Alignments

1. Subsystem root build files (`backend/build.gradle.kts`, `records/build.gradle.kts`, `frontend/build.gradle.kts`, `internal/build.gradle.kts`, `gradle/tools/project-plugins/build.gradle.kts`):
   - Version assigned via `local.versions.version.get()`
2. Root `build.gradle.kts` Dokka block:
   ```kotlin
   dependencies {
       // Records Subsystem
       dokka(local.bundles.records)

       // Tools & Project Plugins
       dokka(local.project.plugins)

       // Backend Subsystem
       dokka(local.bundles.backend)

       // Frontend Subsystem
       dokka(local.bundles.frontend)

       // Internal Subsystem
       dokka(local.bundles.internal)
   }
   ```
3. `frontend/apps/cli/build.gradle.kts`:
   - Replace literal backend strings with `local.backend.job.post.ranking`, `local.backend.email.classification`, `local.backend.service`, `local.backend.ai.client`.
4. Clean up `gradle/libs.versions.toml`:
   - Remove `dev-fnc314-aijobtracker`, `dev-fnc314-aijobtracker-backend`, `records` versions and `dev-fnc314-aijobtracker-records` bundle from `libs.versions.toml`.

---

## 5. Verification Plan
- Run `./gradlew :tests:test` to verify all architecture and consistency rules pass.
- Run `./gradlew buildAll` to verify composite builds assemble cleanly.
