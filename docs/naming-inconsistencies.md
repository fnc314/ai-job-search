# Naming Inconsistencies & Alignment Plan for Version 2.0.0

This document tracks identified naming, branding, and structural inconsistencies across the repository to prepare for seamless alignment in release **v2.0.0**.

---

## 1. Domain & Project Identity

| Artifact / Context | Current Name / Value | Inconsistency | Recommended v2.0.0 Target |
| :--- | :--- | :--- | :--- |
| **GitHub Repository** | `ai-job-search` | Repository is named `ai-job-search`, but user-facing product & dashboard brand as "Job Application Tracker" / `ai-job-tracker`. | Standardize repository and documentation references to `ai-job-tracker` (or establish clear hierarchy: `ai-job-tracker` application within `ai-job-search` platform). |
| **Custom Domain** | `https://ai-job-tracker.fnc314.dev` | Subdomain uses `ai-job-tracker`, diverging from repo slug `ai-job-search`. | Keep `ai-job-tracker.fnc314.dev` as primary public domain; align repo docs and header titles. |
| **Root Settings / Project Name** | `rootProject.name = "FNC314JobSearchTracker"` in `frontend/settings.gradle.kts` | PascalCase legacy name required by Compose Multiplatform resource code generator (`fnc314jobsearchtracker.*`). | Migrate generated resource namespace cleanly to `dev.fnc314.tools.jobtracker.frontend.resources` or `aijobtracker` once CMP allows custom resource package configuration. |
| **CLI & Tools Brand** | Mixed references: `job-tracker`, `ai-job-search`, `jobsearch` | `mise run tracker` vs repo name `ai-job-search` vs internal packages. | Adopt unified product namespace `job-tracker` across all CLI interfaces. |

---

## 2. Kotlin Module & Package Namespaces

| Artifact / Context | Current Name / Value | Inconsistency | Recommended v2.0.0 Target |
| :--- | :--- | :--- | :--- |
| **Package Namespaces** | `dev.fnc314.tools.jobtracker.*` vs `dev.fnc314.tools.jobsearch.records.*` | Records subsystem originally used `jobsearch.records` while frontend/tools used `jobtracker`. | Unify all Kotlin modules under root namespace `dev.fnc314.tools.jobtracker.*` (`jobtracker.records`, `jobtracker.frontend`, etc.). |
| **Gradle Module Paths** | `:records:parser`, `:records:proto` vs `:frontend:core-widgets`, etc. | Distributed multi-project builds (`frontend/settings.gradle.kts` vs root `settings.gradle.kts`). | Consider unifying composite builds or explicit subproject path conventions (`:internal:records:*`, `:internal:apps:frontend:*`). |
| **Maven Coordinate IDs** | `dev.fnc314.tools.jobtracker.tools:ai-job-search_gradle-tools_project-plugins:1.0.0` | Group ID is `jobtracker.tools` but artifact name uses `ai-job-search_gradle-tools_project-plugins`. | Normalize artifact names: `dev.fnc314.tools.jobtracker:project-plugins:2.0.0`. |

---

## 3. Mise Task Groups & Tooling

| Artifact / Context | Current Name / Value | Inconsistency | Recommended v2.0.0 Target |
| :--- | :--- | :--- | :--- |
| **Email Synchronization** | Task prefix `email:*` vs legacy `gmail:*` references in internal scripts and cache dirs | Migrated public interface to `email:*`, but some internal scripts or caches still reference `gmail`. | Complete removal of any legacy `gmail` identifiers in internal paths and variables in favor of `email`. |
| **Search vs Tracker Tasks** | `search:scrape`, `search:rank` vs `tracker:*` | Search tasks operate on discovery, while tracker operates on applications. | Keep separation clear, but provide top-level aliases or unified workflow commands. |
| **Task Documentation Names** | `docs/mise/tasks/admin-release.py.md` vs `docs/mise/tasks/tracker.py.md` | File-based auto-generation creates hyphenated filenames matching script names. | Enhance task docs generator to produce canonical grouped hierarchy (`docs/mise/admin/release.md`). |

---

## 4. UI & Web Dashboard Alignment

| Artifact / Context | Current Name / Value | Inconsistency | Recommended v2.0.0 Target |
| :--- | :--- | :--- | :--- |
| **Web Dashboard Brand** | "AI Job Application Tracker" / "Job Application Records" | Header titles vary between `application-dashboard.html` and `docs/site/templates/landing.html`. | Establish shared Material 3 header component and canonical title across static site and web dashboard. |
| **Color Tokens** | `#2F5597` (Professional Blue) | Used across CV, Cover Letters, and new Material 3 site tokens, but legacy CSS files had slight variations (`#1f3b64`, `#3b82f6`). | Consolidate on `material3-tokens.css` as single source of truth for all web UI surfaces. |
