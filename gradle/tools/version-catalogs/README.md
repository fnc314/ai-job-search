# Version Catalogs — Sync Guide

This directory contains a published Version Catalog artifact that mirrors the root project's dependency catalog. There are **two** `libs.versions.toml` files in this repository, and they must be kept in sync:

| File | Role |
|------|------|
| `gradle/libs.versions.toml` | **Source of truth** — consumed by all Gradle subprojects via `settings.gradle.kts` |
| `gradle/tools/version-catalogs/src/main/resources/libs.versions.toml` | **Published mirror** — distributed as a Maven artifact for external tooling |

## Why Two Files?

- The root catalog (`gradle/libs.versions.toml`) is included by every subproject through `dependencyResolutionManagement { versionCatalogs { create("libs") { from(files("../../libs.versions.toml")) } } }`.
- The tools catalog is published as a standalone Maven artifact (`dev.fnc314.tools.jobtracker.gradle.tools:version-catalogs`) so external scripts and tools can resolve dependency versions without a full Gradle build.

## Sync Rules

### 1. Always Edit the Root Catalog First

Make all changes — new versions, libraries, bundles, or plugins — in `gradle/libs.versions.toml`.

### 2. Mirror Changes to the Tools Catalog

After modifying the root catalog, copy the corresponding changes to `gradle/tools/version-catalogs/src/main/resources/libs.versions.toml`.

### 3. Sections to Sync

When adding or updating entries, check **all four sections**:

- **`[versions]`** — version strings (e.g., `kotlin = "2.4.21"`)
- **`[libraries]`** — library coordinates (e.g., `kotlin-gradle-plugin = { ... }`)
- **`[bundles]`** — library groups (e.g., `compose-ui = [ ... ]`)
- **`[plugins]`** — plugin IDs (e.g., `kotlinJvm = { id = "org.jetbrains.kotlin.jvm", ... }`)

### 4. Verify Sync Before Committing

Use `diff` to compare the two files and confirm only intentional differences remain:

```bash
diff gradle/libs.versions.toml gradle/tools/version-catalogs/src/main/resources/libs.versions.toml
```

Any unexpected differences indicate drift that should be resolved before committing.

## Known Intentional Differences

The tools catalog may include additional entries required by external tooling that are not needed by the Gradle build. Document any such additions with a comment in the tools catalog:

```toml
# External tooling only — not used by Gradle build
markdown-renderer = "0.46.0"
```

## Quick Checklist

When adding a new dependency:

- [ ] Add version to `[versions]` in **root** catalog
- [ ] Add library to `[libraries]` in **root** catalog
- [ ] If applicable, add to a `[bundles]` entry in **root** catalog
- [ ] If applicable, add plugin to `[plugins]` in **root** catalog
- [ ] Mirror the version to `[versions]` in **tools** catalog
- [ ] Mirror the library to `[libraries]` in **tools** catalog
- [ ] Mirror any bundle or plugin changes to **tools** catalog
- [ ] Run `diff` to verify no unintended drift
