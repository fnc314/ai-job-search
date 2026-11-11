---
sessionId: session-261111-025449-1q7s
---

# Analysis

### Current State

The project has **two nearly identical version catalog TOML files**:

1. **`gradle/libs.versions.toml`** (279 lines) — the root catalog, consumed by all builds
2. **`gradle/tools/version-catalogs/src/main/resources/libs.versions.toml`** (275 lines) — a standalone module that is **not consumed by anything**

### How catalogs are actually distributed

All composite builds (`records`, `backend`, `frontend`, `project-plugins`) get their `libs` catalog by pointing directly at the root TOML file:

- **`records/`**, **`backend/`**, **`frontend/`** apply the shared script `gradle/tools/settings-plugins/subsystem.settings.gradle.kts`, which creates a `libs` catalog from `gradle/libs.versions.toml` via path resolution.
- **`gradle/tools/project-plugins/`** has its own `settings.gradle.kts` that also points to `../../libs.versions.toml`.

### The `version-catalogs` module is dead code

The module at `gradle/tools/version-catalogs/` is a standalone Gradle build (with its own `settings.gradle.kts` and `build.gradle.kts`) that publishes the catalog as a Maven artifact. However:

- It is **not included via `includeBuild`** anywhere in `settings.gradle.kts` or any subsystem.
- No dependency in any `build.gradle.kts` or `libs.versions.toml` references `dev.fnc314.tools.jobtracker.gradle.tools`.
- No subsystem consumes it.

### Differences between the two TOML files

The root `gradle/libs.versions.toml` has a few entries the version-catalogs copy lacks:
- `markdown-renderer` version and `markdown-renderer-m3` library
- `compose-material-icons-extended` library and its bundle entry

These are likely the result of drift — the root catalog was updated but the version-catalogs copy was never synced.

### Answer to your question

**Yes — you already are using the root catalog directly.** The `@gradle/tools/version-catalogs` module is unnecessary and can be safely removed. The current architecture (via `subsystem.settings.gradle.kts`) already provides a single source of truth at `gradle/libs.versions.toml` that all composite builds reference.

### Proposed Changes

1. **Remove** `gradle/tools/version-catalogs/` directory entirely (it's unused).
2. **Keep** `gradle/libs.versions.toml` as the single source of truth.
3. **Ensure** the root catalog includes all entries currently only in one file (it already does — it's the superset).

# Delivery Steps

### ✓ Step 1: Remove the unused version-catalogs module
Delete the `gradle/tools/version-catalogs/` directory and all its contents, as it is not consumed by any build in the project.

### ✓ Step 2: Verify all builds still resolve the libs catalog
Run `./gradlew projects` and `./gradlew dependencies` (or `mise run records:build` / `frontend:build`) to confirm all composite builds still resolve the `libs` catalog from `gradle/libs.versions.toml` without issues.