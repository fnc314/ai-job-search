# Implementation Plan: Plan Files Consolidation, Sorting & Repository Gitignore Alignment

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** repository-wide (root .agents/plans, .gitignore)  

---

## Overview & Goal
Ensure that all architecture, design, and migration plan documents across the entire repository (as well as related user environment sessions) are correctly categorized and persisted inside their respective `.agents/plans/` directories, and universally align `.gitignore` across the entire repository tree:
1. **`ui/kmp` specific plans** reside exclusively in [`ui/kmp/.agents/plans/`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/.agents/plans/).
2. **Repository-wide / root plans** reside exclusively in [`/.agents/plans/`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/).
3. Legacy plans stored in `.junie/plans/` and `ui/kmp/.junie/plans/` are moved/copied to their canonical `.agents/plans/` locations.
4. Related plans from prior Antigravity CLI sessions (`~/.gemini/antigravity-cli/brain/71ca985f-...`) are brought into `ui/kmp/.agents/plans/`.
5. Redundant/mislocated plans in root `.agents/plans/` that pertain strictly to `ui/kmp` are removed from the root directory.
6. This plan itself (`plan-files-consolidation-and-gitignore-alignment.md`) is housed in [`/.agents/plans/`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/) as a repository-wide plan.
7. **Universal `.gitignore` alignment**: Update root `.gitignore` to comprehensively ignore `.idea`, `.gradle`, `.kotlin`, `build`, and `dokka` documentation directories throughout the entire repository tree, cleaning up fragmented path-specific rules.
8. Index tables in [`/.agents/plans/README.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/README.md) and [`ui/kmp/.agents/plans/README.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/.agents/plans/README.md) are comprehensively populated.
9. Workspace rules in [`AGENTS.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/AGENTS.md) and [`ui/kmp/AGENTS.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/AGENTS.md) are synchronized.

---

## Gitignore Universal Alignment Specification

### Current State
Root `.gitignore` has scattered, piecemeal rules:
- Line 38: `.idea/` (does not explicitly cover nested modules or IDE projects).
- Line 149-150: `records/**/build/`, `records/.gradle/`.
- Line 178-184: `ui/kmp/build/`, `ui/kmp/.gradle/`, `ui/kmp/.idea/`, `ui/kmp/gradle/tools/.gradle/`, `ui/kmp/gradle/tools/build/`, `ui/kmp/gradle/tools/project-plugins/build/`, `ui/kotlin-toolchain/build/`.
- `.kotlin` is **not ignored anywhere**.

### Proposed Update
Replace scattered rules with clean, universal directory ignores:
```gitignore
# IDE & Tooling
.idea/
**/.idea/

# Gradle, Kotlin & Build output throughout repository
.gradle/
**/.gradle/
.kotlin/
**/.kotlin/
build/
**/build/

# Dokka documentation output
dokka/
**/dokka/
```
Remove redundant piecemeal rules (`records/**/build/`, `records/.gradle/`, `ui/kmp/build/`, `ui/kmp/.gradle/`, `ui/kmp/.idea/`, etc.) from root `.gitignore`.

---

## Detailed Plan Audit & Findings from `~/.gemini` & `~/.junie`

### A. Root User Environment Audit (`~/.gemini` & `~/.junie`)

1. **`~/.junie` Investigation**:
   - **Finding**: No standalone plan markdown files exist in `~/.junie`.
   - **Details**: `~/.junie/sessions/` contains execution transcripts (`transcript.md`), exit status checkpoints, and skills. All plan markdown files created during Junie sessions were deposited directly into workspace folders (`/.junie/plans/` and `ui/kmp/.junie/plans/`), which are captured below.

2. **`~/.gemini/antigravity-ide/brain/` Investigation**:
   - **Finding**: 7 `implementation_plan.md` files exist across IDE session brains.
   - **Disambiguation / Concern**: All 7 files belong to a completely distinct personal project ([`fnc314.github.io`](file:///Users/fnc314/Code/websites/fnc314.github.io) / `fnc314.com`), covering Lit components, Bento Box UI, `cem.jsonc`, `word-tag`, and YouTube Music widget. None belong to `waleedtarbosh-ai-job-search`. **They will be left untouched.**

3. **`~/.gemini/antigravity-cli/brain/` Investigation**:
   - **Finding**: Prior Antigravity session `71ca985f-5a1e-49fd-a6f3-252a0b91df1e` contains **5 completed `ui/kmp` architecture plans** that have not yet been moved into `ui/kmp/.agents/plans/`:
     - `clean-architecture-datasources-repositories-plan.md`
     - `custom-gradle-plugins-architecture-plan.md`
     - `testing-bundles-plan.md`
     - `unit-tests-coverage-plan.md`
     - `module-readmes-plan.md`
   - **Action**: Ingest these 5 files into `ui/kmp/.agents/plans/` to complete the architectural history of `ui/kmp`.

4. **In-Repo Root Plans (`docs/tracker-migration-plan.md` & `ai-job-search-plan.md`)**:
   - `docs/tracker-migration-plan.md` details the CSV-to-Firestore migration. It will be indexed in `/.agents/plans/README.md`.
   - `ai-job-search-plan.md` at repo root is already referenced in `/.agents/plans/README.md`.

---

## Duplication Analysis & Version Comparison

Every duplicate candidate across `/.agents/plans`, `ui/kmp/.agents/plans`, `/.junie/plans`, `ui/kmp/.junie/plans`, and `~/.gemini/.../71ca985f-...` was hashed (SHA-256) and compared by modification timestamp and git status:

### 1. `dokka-and-comprehensive-kdocs-plan.md`
- **`ui/kmp/.agents/plans/`**: SHA `dbeb5453738e` (19,821 bytes, latest edited in session). **Canonical Location.**
- **`/.agents/plans/`**: SHA `dbeb5453738e` (Identical content). **Misplaced in root directory.**
  - **Action**: Delete `/Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/dokka-and-comprehensive-kdocs-plan.md`.
- **`~/.gemini/.../71ca985f/...`**: SHA `5f9be647d470` (19,668 bytes). Older draft prior to composite build refinement.

### 2. `m3-theming-adaptive-layout-and-widgets.md`
- **`ui/kmp/.agents/plans/`**: SHA `ab117c0491e6` (5,084 bytes). **Canonical Location.**
- **`/.agents/plans/`**: SHA `ab117c0491e6` (Identical content). **Misplaced in root directory.**
  - **Action**: Delete `/Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/m3-theming-adaptive-layout-and-widgets.md`.
- **`ui/kmp/.junie/plans/`**: SHA `ab117c0491e6` (Identical content).
  - **Action**: Delete from `ui/kmp/.junie/plans/`.

### 3. `repackaging-shared-and-job-applications-modularization.md`
- **`ui/kmp/.agents/plans/`**: SHA `8f0db795e6ad` (13,968 bytes). **Latest & Completed version** with all phases verified (`[x]`). **Canonical Location.**
- **`/.agents/plans/`**: SHA `6ff4bfba8834` (13,966 bytes). **Older uncompleted draft** (`[ ]`). **Misplaced in root directory.**
  - **Action**: Delete `/Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/repackaging-shared-and-job-applications-modularization.md`.
- **`ui/kmp/.junie/plans/`**: SHA `8f0db795e6ad` (Identical to latest).
  - **Action**: Delete from `ui/kmp/.junie/plans/`.

### 4. `ui-flow-improvements-and-modularization.md`
- **`ui/kmp/.agents/plans/`**: SHA `670d86aee2fa` (5,558 bytes). **Canonical Location.**
- **`/.agents/plans/`**: SHA `670d86aee2fa` (Identical content). **Misplaced in root directory.**
  - **Action**: Delete `/Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.agents/plans/ui-flow-improvements-and-modularization.md`.
- **`ui/kmp/.junie/plans/`**: SHA `670d86aee2fa` (Identical content).
  - **Action**: Delete from `ui/kmp/.junie/plans/`.

### 5. `room3-platform-support-and-caching-plan.md`
- **`ui/kmp/.agents/plans/`**: SHA `5b7aa29f4e52` (29,943 bytes). **Canonical Location.**
- **`~/.gemini/.../71ca985f/...`**: SHA `5b7aa29f4e52` (Identical content).

---

## Target Plan Categorization

### 1. Root Repository Plans (Target: `/.agents/plans/`)
These plans concern repository infrastructure, data models, Python CLI/TUI tools, contracts, and general tooling:

| Plan File | Source Directory | Target Location | Description |
| :--- | :--- | :--- | :--- |
| `plan-files-consolidation-and-gitignore-alignment.md` | In-repo root | `/.agents/plans/` | Plan consolidation, duplication cleanup & universal gitignore |
| `add-job-url-fields-to-tui-and-cli.md` | `/.junie/plans/` | `/.agents/plans/` | Job URL field addition in TUI and Python CLI |
| `firestore-data-optimization.md` | `/.junie/plans/` | `/.agents/plans/` | Firestore data optimization & schema indexing |
| `kmp-proto-contracts.md` | `/.junie/plans/` | `/.agents/plans/` | Top-level repo restructuring, Protobuf contracts & parser |
| `recruiter-outreach-tracking.md` | `/.junie/plans/` | `/.agents/plans/` | Recruiter outreach tracking data schema & CLI |
| `tui-log-rotation-and-cleanup.md` | `/.junie/plans/` | `/.agents/plans/` | Curses TUI log rotation and cleanup mechanism |

### 2. Kotlin Multiplatform Plans (Target: `ui/kmp/.agents/plans/`)
These plans concern KMP modules, Compose Multiplatform UI, MVI, Material 3, Room, and Android/Desktop/Web targets:

| Plan File | Source Location | Action Needed |
| :--- | :--- | :--- |
| `add-job-application-url-and-records-dep.md` | `ui/kmp/.junie/plans/` | Copy to `ui/kmp/.agents/plans/` |
| `create-core-modules-and-notes-widget.md` | `ui/kmp/.junie/plans/` | Copy to `ui/kmp/.agents/plans/` |
| `firebase-android-and-multiplatform-integration.md` | `ui/kmp/.junie/plans/` | Copy to `ui/kmp/.agents/plans/` |
| `fix-android-defects.md` | `ui/kmp/.junie/plans/` | Copy to `ui/kmp/.agents/plans/` |
| `integrate-records-fix-icons.md` | `ui/kmp/.junie/plans/` | Copy to `ui/kmp/.agents/plans/` |
| `kmp-mobile-tracker-ui.md` | `ui/kmp/.junie/plans/` | Copy to `ui/kmp/.agents/plans/` |
| `clean-architecture-datasources-repositories-plan.md` | `~/.gemini/.../71ca985f/` | Copy to `ui/kmp/.agents/plans/` |
| `custom-gradle-plugins-architecture-plan.md` | `~/.gemini/.../71ca985f/` | Copy to `ui/kmp/.agents/plans/` |
| `testing-bundles-plan.md` | `~/.gemini/.../71ca985f/` | Copy to `ui/kmp/.agents/plans/` |
| `unit-tests-coverage-plan.md` | `~/.gemini/.../71ca985f/` | Copy to `ui/kmp/.agents/plans/` |
| `module-readmes-plan.md` | `~/.gemini/.../71ca985f/` | Copy to `ui/kmp/.agents/plans/` |
| `build-logs-and-compiler-warnings-triage-plan.md` | `ui/kmp/.agents/plans/` | Already present |
| `compose-resources-migration-plan.md` | `ui/kmp/.agents/plans/` | Already present |
| `dokka-and-comprehensive-kdocs-plan.md` | `ui/kmp/.agents/plans/`, `/.agents/plans/` | Retain in `ui/kmp/.agents/plans/`; remove duplicate from `/.agents/plans/` |
| `m3-theming-adaptive-layout-and-widgets.md` | `ui/kmp/.agents/plans/`, `/.agents/plans/`, `ui/kmp/.junie/plans/` | Retain in `ui/kmp/.agents/plans/`; remove duplicate from `/.agents/plans/` |
| `mvi-architecture-and-ui-model-separation-plan.md` | `ui/kmp/.agents/plans/` | Already present |
| `platform-agnosticism-renaming-plan.md` | `ui/kmp/.agents/plans/` | Already present |
| `repackaging-shared-and-job-applications-modularization.md` | `ui/kmp/.agents/plans/`, `/.agents/plans/`, `ui/kmp/.junie/plans/` | Retain in `ui/kmp/.agents/plans/`; remove duplicate from `/.agents/plans/` |
| `room3-platform-support-and-caching-plan.md` | `ui/kmp/.agents/plans/` | Already present |
| `root-scaffold-and-navigation-enhancement-plan.md` | `ui/kmp/.agents/plans/` | Already present |
| `thinning-apps-core-and-core-navigation.md` | `ui/kmp/.agents/plans/` | Already present |
| `ui-flow-improvements-and-modularization.md` | `ui/kmp/.agents/plans/`, `/.agents/plans/`, `ui/kmp/.junie/plans/` | Retain in `ui/kmp/.agents/plans/`; remove duplicate from `/.agents/plans/` |

---

## Migration & Consolidation Execution Steps

1. **Delete Misplaced Copies from Root `/.agents/plans/`**:
   - `rm /.agents/plans/dokka-and-comprehensive-kdocs-plan.md`
   - `rm /.agents/plans/m3-theming-adaptive-layout-and-widgets.md`
   - `rm /.agents/plans/repackaging-shared-and-job-applications-modularization.md`
   - `rm /.agents/plans/ui-flow-improvements-and-modularization.md`

2. **Move Root Repository Plans from `/.junie/plans/` to `/.agents/plans/` (`git mv`)**:
   - `git mv .junie/plans/add-job-url-fields-to-tui-and-cli.md .agents/plans/`
   - `git mv .junie/plans/firestore-data-optimization.md .agents/plans/`
   - `git mv .junie/plans/kmp-proto-contracts.md .agents/plans/`
   - `git mv .junie/plans/recruiter-outreach-tracking.md .agents/plans/`
   - `git mv .junie/plans/tui-log-rotation-and-cleanup.md .agents/plans/`

3. **Consolidate `ui/kmp` Plans**:
   - Delete the 3 identical duplicate files from `ui/kmp/.junie/plans/`:
     - `rm ui/kmp/.junie/plans/m3-theming-adaptive-layout-and-widgets.md`
     - `rm ui/kmp/.junie/plans/repackaging-shared-and-job-applications-modularization.md`
     - `rm ui/kmp/.junie/plans/ui-flow-improvements-and-modularization.md`
   - Move the remaining 6 unique files from `ui/kmp/.junie/plans/` to `ui/kmp/.agents/plans/` (`git mv`):
     - `git mv ui/kmp/.junie/plans/add-job-application-url-and-records-dep.md ui/kmp/.agents/plans/`
     - `git mv ui/kmp/.junie/plans/create-core-modules-and-notes-widget.md ui/kmp/.agents/plans/`
     - `git mv ui/kmp/.junie/plans/firebase-android-and-multiplatform-integration.md ui/kmp/.agents/plans/`
     - `git mv ui/kmp/.junie/plans/fix-android-defects.md ui/kmp/.agents/plans/`
     - `git mv ui/kmp/.junie/plans/integrate-records-fix-icons.md ui/kmp/.agents/plans/`
     - `git mv ui/kmp/.junie/plans/kmp-mobile-tracker-ui.md ui/kmp/.agents/plans/`
   - Ingest the 5 plans from prior session `~/.gemini/antigravity-cli/brain/71ca985f-.../`:
     - `clean-architecture-datasources-repositories-plan.md`
     - `custom-gradle-plugins-architecture-plan.md`
     - `testing-bundles-plan.md`
     - `unit-tests-coverage-plan.md`
     - `module-readmes-plan.md`

4. **Universal `.gitignore` Alignment**:
   - Update `/.gitignore` to ignore `.idea/`, `**/.idea/`, `.gradle/`, `**/.gradle/`, `.kotlin/`, `**/.kotlin/`, `build/`, `**/build/`, `dokka/`, and `**/dokka/`.
   - Remove redundant narrow path rules.

5. **Update Index READMEs & Documentation**:
   - Update `/.agents/plans/README.md` with complete table of root plans.
   - Update `ui/kmp/.agents/plans/README.md` with complete table of all 22 KMP plans.
   - Update root `AGENTS.md` to reference `.agents/plans/` for repository-wide plans.
