# Implementation Plan: Relocate Dokka Plan & Date/Timestamp Reorganization of `.agents/plans/`

## Goal Description
1. Relocate [`dokka-and-comprehensive-kdocs-plan.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/.agents/plans/dokka-and-comprehensive-kdocs-plan.md) from the subsystem directory `ui/kmp/.agents/plans/` to the repository-wide directory `.agents/plans/` because its scope affects both `records` (`records-proto`, `records-parser`) and `ui/kmp` (including `gradle/tools/project-plugins`).
2. Store this plan document [`plan-relocation-and-index-alignment.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/plan-relocation-and-index-alignment.md) directly in `.agents/plans/` as a permanent repository-wide architectural record.
3. For each `.agents/plans/` directory (both root `.agents/plans/` and `ui/kmp/.agents/plans/`), reorganize all plan `.md` files (excluding `README.md`) using their `git` or filesystem creation timestamp into `YYYY-MM-DD/` subdirectories with 24-hour clock timestamp-prefixed filenames (`HHMMSS_plan-name.md` with HH in `00`-`23`).
4. Update the registries and index tables in `README.md` across both `.agents/plans/` directories to maintain clean, navigable, and valid links.

---

## User Review Required

> [!IMPORTANT]
> **Directory Restructuring & File Path Conventions**:
> Every existing plan `.md` (except the root `README.md` index) in both `.agents/plans/` locations will be migrated to:
> ```
> .agents/plans/YYYY-MM-DD/HHMMSS_plan-name.md
> ```
> - Timestamps are determined via `git log --diff-filter=A --follow --format="%ad" --date=format:"%Y-%m-%d %H:%M:%S"` with fallback to filesystem creation/birth time (`stat`).
> - Timestamps use 24-hour clock formatting (`00`-`23` for `HH`).
> - The `README.md` index in each directory remains at the root of `.agents/plans/` and updates its markdown table links to the relative `YYYY-MM-DD/HHMMSS_plan-name.md` paths.

---

## Proposed Changes

### Component 1: File Relocations to Root `.agents/plans/`

#### [NEW] `.agents/plans/.../HHMMSS_dokka-and-comprehensive-kdocs-plan.md`
- Move the Dokka implementation plan from `ui/kmp/.agents/plans/` to the repository-wide `.agents/plans/` directory under its creation timestamp date folder:
  - Timestamp: `2026-11-05 16:27:01`
  - Target Path: `.agents/plans/2026-11-05/162701_dokka-and-comprehensive-kdocs-plan.md`

#### [DELETE] `ui/kmp/.agents/plans/dokka-and-comprehensive-kdocs-plan.md`
- Remove the local subsystem copy from `ui/kmp/.agents/plans/`.

#### [NEW] `.agents/plans/.../HHMMSS_plan-relocation-and-index-alignment.md`
- Store this technical plan document directly in `.agents/plans/` under today's timestamp:
  - Timestamp: `2026-11-05 17:38:50`
  - Target Path: `.agents/plans/2026-11-05/173850_plan-relocation-and-index-alignment.md`

---

### Component 2: Reorganization of Root `.agents/plans/`

Reorganize all existing plans in `.agents/plans/` into `YYYY-MM-DD/HHMMSS_plan-name.md`:

| Original File | Created Timestamp | Target Organized Path |
| :--- | :--- | :--- |
| `add-job-url-fields-to-tui-and-cli.md` | `2026-11-04 01:29:18` | `.agents/plans/2026-11-04/012918_add-job-url-fields-to-tui-and-cli.md` |
| `detekt-and-konsist-integration-plan.md` | `2026-11-05 16:55:00` | `.agents/plans/2026-11-05/165500_detekt-and-konsist-integration-plan.md` |
| `firestore-data-optimization.md` | `2026-10-31 18:53:11` | `.agents/plans/2026-10-31/185311_firestore-data-optimization.md` |
| `kmp-proto-contracts.md` | `2026-11-02 06:45:00` | `.agents/plans/2026-11-02/064500_kmp-proto-contracts.md` |
| `plan-files-consolidation-and-gitignore-alignment.md` | `2026-11-05 16:39:15` | `.agents/plans/2026-11-05/163915_plan-files-consolidation-and-gitignore-alignment.md` |
| `recruiter-outreach-tracking.md` | `2026-11-02 17:21:44` | `.agents/plans/2026-11-02/172144_recruiter-outreach-tracking.md` |
| `tui-log-rotation-and-cleanup.md` | `2026-11-01 17:43:55` | `.agents/plans/2026-11-01/174355_tui-log-rotation-and-cleanup.md` |
| `dokka-and-comprehensive-kdocs-plan.md` | `2026-11-05 16:27:01` | `.agents/plans/2026-11-05/162701_dokka-and-comprehensive-kdocs-plan.md` |
| `plan-relocation-and-index-alignment.md` | `2026-11-05 17:38:50` | `.agents/plans/2026-11-05/173850_plan-relocation-and-index-alignment.md` |

---

### Component 3: Reorganization of Subsystem `ui/kmp/.agents/plans/`

Reorganize all existing plans in `ui/kmp/.agents/plans/` into `YYYY-MM-DD/HHMMSS_plan-name.md`:

| Original File | Created Timestamp | Target Organized Path |
| :--- | :--- | :--- |
| `add-job-application-url-and-records-dep.md` | `2026-11-04 00:55:24` | `ui/kmp/.agents/plans/2026-11-04/005524_add-job-application-url-and-records-dep.md` |
| `build-logs-and-compiler-warnings-triage-plan.md` | `2026-11-05 16:03:46` | `ui/kmp/.agents/plans/2026-11-05/160346_build-logs-and-compiler-warnings-triage-plan.md` |
| `clean-architecture-datasources-repositories-plan.md` | `2026-11-05 16:40:56` | `ui/kmp/.agents/plans/2026-11-05/164056_clean-architecture-datasources-repositories-plan.md` |
| `compose-resources-migration-plan.md` | `2026-11-05 15:47:29` | `ui/kmp/.agents/plans/2026-11-05/154729_compose-resources-migration-plan.md` |
| `create-core-modules-and-notes-widget.md` | `2026-11-03 20:20:11` | `ui/kmp/.agents/plans/2026-11-03/202011_create-core-modules-and-notes-widget.md` |
| `custom-gradle-plugins-architecture-plan.md` | `2026-11-05 16:40:56` | `ui/kmp/.agents/plans/2026-11-05/164056_custom-gradle-plugins-architecture-plan.md` |
| `DRAFT-code-refactor.md` | `2026-11-05 17:02:26` | `ui/kmp/.agents/plans/2026-11-05/170226_DRAFT-code-refactor.md` |
| `firebase-android-and-multiplatform-integration.md` | `2026-11-04 00:55:24` | `ui/kmp/.agents/plans/2026-11-04/005524_firebase-android-and-multiplatform-integration.md` |
| `fix-android-defects.md` | `2026-11-03 19:46:04` | `ui/kmp/.agents/plans/2026-11-03/194604_fix-android-defects.md` |
| `integrate-records-fix-icons.md` | `2026-11-04 07:00:45` | `ui/kmp/.agents/plans/2026-11-04/070045_integrate-records-fix-icons.md` |
| `kmp-mobile-tracker-ui.md` | `2026-11-02 21:11:52` | `ui/kmp/.agents/plans/2026-11-02/211152_kmp-mobile-tracker-ui.md` |
| `m3-theming-adaptive-layout-and-widgets.md` | `2026-11-05 15:50:22` | `ui/kmp/.agents/plans/2026-11-05/155022_m3-theming-adaptive-layout-and-widgets.md` |
| `module-readmes-plan.md` | `2026-11-05 16:40:56` | `ui/kmp/.agents/plans/2026-11-05/164056_module-readmes-plan.md` |
| `mvi-architecture-and-ui-model-separation-plan.md` | `2026-11-05 15:47:58` | `ui/kmp/.agents/plans/2026-11-05/154758_mvi-architecture-and-ui-model-separation-plan.md` |
| `platform-agnosticism-renaming-plan.md` | `2026-11-05 15:48:28` | `ui/kmp/.agents/plans/2026-11-05/154828_platform-agnosticism-renaming-plan.md` |
| `repackaging-shared-and-job-applications-modularization.md` | `2026-11-05 15:50:05` | `ui/kmp/.agents/plans/2026-11-05/155005_repackaging-shared-and-job-applications-modularization.md` |
| `room3-platform-support-and-caching-plan.md` | `2026-11-05 16:27:21` | `ui/kmp/.agents/plans/2026-11-05/162721_room3-platform-support-and-caching-plan.md` |
| `root-scaffold-and-navigation-enhancement-plan.md` | `2026-11-05 15:49:36` | `ui/kmp/.agents/plans/2026-11-05/154936_root-scaffold-and-navigation-enhancement-plan.md` |
| `testing-bundles-plan.md` | `2026-11-05 16:40:56` | `ui/kmp/.agents/plans/2026-11-05/164056_testing-bundles-plan.md` |
| `thinning-apps-core-and-core-navigation.md` | `2026-11-05 15:49:19` | `ui/kmp/.agents/plans/2026-11-05/154919_thinning-apps-core-and-core-navigation.md` |
| `ui-flow-improvements-and-modularization.md` | `2026-11-05 15:50:45` | `ui/kmp/.agents/plans/2026-11-05/155045_ui-flow-improvements-and-modularization.md` |
| `unit-tests-coverage-plan.md` | `2026-11-05 16:40:56` | `ui/kmp/.agents/plans/2026-11-05/164056_unit-tests-coverage-plan.md` |

---

### Component 4: Registry Updates in `README.md` Files

#### [MODIFY] `.agents/plans/README.md`
Update table with the reorganized relative paths:
```markdown
# Repository Architectural Plans

This directory holds architecture, system design, and migration plans for the repository.

### Subsystem Plan Directories
- **Kotlin Multiplatform UI Workspace:** [`ui/kmp/.agents/plans/`](../../ui/kmp/.agents/plans/README.md) houses all KMP UI, MVI, design system, and multiplatform feature architecture plans.

---

### Repository-Wide Plans

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`dokka-and-comprehensive-kdocs-plan.md`](./2026-11-05/162701_dokka-and-comprehensive-kdocs-plan.md) | Completed | Dokka 2.3.0 unified multi-project setup & exhaustive KDocs across records & ui/kmp |
| [`plan-relocation-and-index-alignment.md`](./2026-11-05/173850_plan-relocation-and-index-alignment.md) | In Review | Reorganize plans into YYYY-MM-DD/HHMMSS_plan-name.md and align registries |
| [`detekt-and-konsist-integration-plan.md`](./2026-11-05/165500_detekt-and-konsist-integration-plan.md) | In Review | Integration of Detekt 1.23.8 & Konsist 0.18.1 across `records` & `ui/kmp` |
| [`plan-files-consolidation-and-gitignore-alignment.md`](./2026-11-05/163915_plan-files-consolidation-and-gitignore-alignment.md) | Completed | Plan files reorganization, duplicate cleanup, and universal `.gitignore` alignment |
| [`kmp-proto-contracts.md`](./2026-11-02/064500_kmp-proto-contracts.md) | Completed | Top-level repo restructuring, Protobuf type contracts, and shared parsing module |
| [`firestore-data-optimization.md`](./2026-10-31/185311_firestore-data-optimization.md) | Completed | Firestore schema optimization, projection queries, and index alignment |
| [`recruiter-outreach-tracking.md`](./2026-11-02/172144_recruiter-outreach-tracking.md) | Completed | Recruiter outreach data models, CLI subcommands, and pipeline tracking |
| [`add-job-url-fields-to-tui-and-cli.md`](./2026-11-04/012918_add-job-url-fields-to-tui-and-cli.md) | Completed | Job URL fields in tracker schema, Curses TUI, and Python CLI tools |
| [`tui-log-rotation-and-cleanup.md`](./2026-11-01/174355_tui-log-rotation-and-cleanup.md) | Completed | Curses TUI session log rotation, retention limit, and disk cleanup |
| [`docs/tracker-migration-plan.md`](../../docs/tracker-migration-plan.md) | Completed | 10-phase authoritative migration from CSV tracker to Firestore store |
| [`ai-job-search-plan.md`](../../ai-job-search-plan.md) | Canonical | Universal multi-agent architecture and single source of truth specifications |
```

#### [MODIFY] `ui/kmp/.agents/plans/README.md`
Update table with reorganized relative paths:
```markdown
# Kotlin Multiplatform Workspace Architecture & Design Plans

This directory contains design and implementation plans for the `ui/kmp` project.

> [!NOTE]
> For repository-wide cross-subsystem plans that span `records` and `ui/kmp` (such as Dokka documentation aggregation and Detekt/Konsist linting), refer to the root architectural plans directory at [`.agents/plans/`](../../.agents/plans/README.md).

| Plan | Status | Scope / Focus Area |
| :--- | :--- | :--- |
| [`add-job-application-url-and-records-dep.md`](./2026-11-04/005524_add-job-application-url-and-records-dep.md) | Completed | Job URL navigation support and records composite build integration |
| [`build-logs-and-compiler-warnings-triage-plan.md`](./2026-11-05/160346_build-logs-and-compiler-warnings-triage-plan.md) | Completed | Build logs and compiler warnings triage & resolution |
| [`clean-architecture-datasources-repositories-plan.md`](./2026-11-05/164056_clean-architecture-datasources-repositories-plan.md) | Completed | Clean Architecture layer decoupling: DataSources in core, Repositories in features |
| [`compose-resources-migration-plan.md`](./2026-11-05/154729_compose-resources-migration-plan.md) | Completed | Migration of all UI literals to `composeResources` |
| [`create-core-modules-and-notes-widget.md`](./2026-11-03/202011_create-core-modules-and-notes-widget.md) | Completed | Core widget extraction (`NotesWidget`, `StatusBadge`, `FitBadge`) |
| [`custom-gradle-plugins-architecture-plan.md`](./2026-11-05/164056_custom-gradle-plugins-architecture-plan.md) | Completed | Custom convention plugins in `gradle/tools/project-plugins` |
| [`DRAFT-code-refactor.md`](./2026-11-05/170226_DRAFT-code-refactor.md) | Draft | Self-contained Enum relationships & companion lookup map refactor proposal |
| [`firebase-android-and-multiplatform-integration.md`](./2026-11-04/005524_firebase-android-and-multiplatform-integration.md) | Completed | GitLive Firebase SDK multiplatform data store implementation |
| [`fix-android-defects.md`](./2026-11-03/194604_fix-android-defects.md) | Completed | Android runtime, build, and edge-to-edge UI defect fixes |
| [`integrate-records-fix-icons.md`](./2026-11-04/070045_integrate-records-fix-icons.md) | Completed | Protobuf records integration and Material icon fixes |
| [`kmp-mobile-tracker-ui.md`](./2026-11-02/211152_kmp-mobile-tracker-ui.md) | Completed | Foundation KMP tracker architecture, models, and initial UI screens |
| [`m3-theming-adaptive-layout-and-widgets.md`](./2026-11-05/155022_m3-theming-adaptive-layout-and-widgets.md) | Completed | Material 3 token system, dynamic colors & adaptive layouts |
| [`module-readmes-plan.md`](./2026-11-05/164056_module-readmes-plan.md) | Completed | Module `README.md` documentation standard & verification script |
| [`mvi-architecture-and-ui-model-separation-plan.md`](./2026-11-05/154758_mvi-architecture-and-ui-model-separation-plan.md) | Completed | MVI state/intent architecture & UI model decoupling |
| [`platform-agnosticism-renaming-plan.md`](./2026-11-05/154828_platform-agnosticism-renaming-plan.md) | Completed | Platform-agnostic module renaming (`apps/core`, `features/*`) |
| [`repackaging-shared-and-job-applications-modularization.md`](./2026-11-05/155005_repackaging-shared-and-job-applications-modularization.md) | Completed | Feature packaging and modularization (`features/job-applications-*`) |
| [`room3-platform-support-and-caching-plan.md`](./2026-11-05/162721_room3-platform-support-and-caching-plan.md) | Completed | Room 3 multiplatform platform support, Uuid7 & caching |
| [`root-scaffold-and-navigation-enhancement-plan.md`](./2026-11-05/154936_root-scaffold-and-navigation-enhancement-plan.md) | Completed | Navigation backstack & adaptive scaffold enhancement |
| [`testing-bundles-plan.md`](./2026-11-05/164056_testing-bundles-plan.md) | Completed | Multiplatform testing catalog bundles & convention plugin |
| [`thinning-apps-core-and-core-navigation.md`](./2026-11-05/154919_thinning-apps-core-and-core-navigation.md) | Completed | Decoupling navigation routes to feature modules |
| [`ui-flow-improvements-and-modularization.md`](./2026-11-05/155045_ui-flow-improvements-and-modularization.md) | Completed | UI screen flows and widget extractions |
| [`unit-tests-coverage-plan.md`](./2026-11-05/164056_unit-tests-coverage-plan.md) | Completed | 5:1 Unit test coverage across platform-agnostic business logic |
```

---

## Verification Plan

### Automated Verification
1. **File System Verification**:
   - Verify all `.md` files exist in their respective `YYYY-MM-DD/HHMMSS_*.md` paths.
   - Verify no orphaned flat `.md` files remain in `.agents/plans/` (except `README.md`).
   - Verify no orphaned flat `.md` files remain in `ui/kmp/.agents/plans/` (except `README.md`).
2. **Link Verification**:
   - Verify all links in `.agents/plans/README.md` resolve to existing files on disk.
   - Verify all links in `ui/kmp/.agents/plans/README.md` resolve to existing files on disk.

### Manual Verification
- Confirm that both `README.md` indices present clear, organized tables and that all links navigate directly to their corresponding date-stamped plan files.
