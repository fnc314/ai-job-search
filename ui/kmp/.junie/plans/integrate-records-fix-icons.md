---
sessionId: session-261104-065552-id32
---

# Requirements

### Overview & Goals

Two tasks:
1. **Integrate the `records` project** — ensure both `records-proto` and `records-parser` are available as dependencies in the KMP project.
2. **Fix IconButton rendering** — two screens have bare `Icons.*` expressions inside `IconButton`/`FloatingActionButton` content lambdas instead of proper `Icon(imageVector, contentDescription)` composables, causing icons to not render on device.

### Scope

**In Scope:**
- Add `records-parser` to the version catalog and wire it into `shared/build.gradle.kts` (`jvmMain`), alongside the existing `records-proto`.
- Fix 8 broken icon instances across 2 files.

**Out of Scope:**
- Adding records dependencies to `commonMain` (records is JVM-only).
- Changing icon themes or styles.

# Technical Design

### Current Implementation

**Records:** `records-proto` is in `libs.versions.toml` (line 85) and used in `shared/build.gradle.kts` `jvmMain` (line 92). `records-parser` is published to `../../.m2` but not referenced in the KMP project.

**Icon bug:** `JobApplicationDetailScreen.kt` (lines 88, 102, 114, 126) and `JobApplicationsListScreen.kt` (lines 77, 88, 99, 113) contain bare `Icons.*` expressions inside `IconButton`/`FloatingActionButton` content lambdas. The `ImageVector` is evaluated but never drawn. Five other files use the correct `Icon(...)` pattern.

### Proposed Changes

**1. Add `records-parser` dependency:**
- Add to `libs.versions.toml`: `records-parser = { group = "dev.fnc314.tools.jobsearch.records", name = "records-parser", version.ref = "records" }`
- Add `implementation(libs.records.parser)` to `jvmMain` in `shared/build.gradle.kts`.

**2. Fix `JobApplicationDetailScreen.kt`** — 4 instances:
- Line 88: `Icons.AutoMirrored.Sharp.ArrowBack` into `Icon(Icons.AutoMirrored.Sharp.ArrowBack, contentDescription = "Back")`
- Line 102: `Icons.Sharp.Notifications` into `Icon(Icons.Sharp.Notifications, contentDescription = "Update status")`
- Line 114: `Icons.Sharp.Edit` into `Icon(Icons.Sharp.Edit, contentDescription = "Record event")`
- Line 126: `Icons.Default.Build` into `Icon(Icons.Default.Build, contentDescription = "Update fit score")`

**3. Fix `JobApplicationsListScreen.kt`** — 4 instances:
- Line 77: `Icons.Sharp.Edit` into `Icon(Icons.Sharp.Edit, contentDescription = "View pending edits")`
- Line 88: `Icons.Sharp.Settings` into `Icon(Icons.Sharp.Settings, contentDescription = "Open settings")`
- Line 99: `Icons.Sharp.Refresh` into `Icon(Icons.Sharp.Refresh, contentDescription = "Refresh")`
- Line 113: `Icons.Sharp.Add` into `Icon(Icons.Sharp.Add, contentDescription = "Add")`

### Files Modified

- `gradle/libs.versions.toml` — Add `records-parser` library entry
- `shared/build.gradle.kts` — Add `records-parser` to `jvmMain`
- `shared/.../ui/detail/JobApplicationDetailScreen.kt` — Fix 4 icon renderings
- `shared/.../ui/list/JobApplicationsListScreen.kt` — Fix 4 icon renderings

# Delivery Steps

### ✓ Step 1: Add records-parser dependency
Wire `records-parser` into the KMP project's version catalog and `shared` module.

- Add `records-parser` library entry to `gradle/libs.versions.toml` under `[libraries]`, referencing the existing `records` version (`1.0.0`).
- Add `implementation(libs.records.parser)` to the `jvmMain` source set in `shared/build.gradle.kts`, alongside the existing `records-proto` dependency.

### ✓ Step 2: Fix IconButton icon rendering in both affected screens
Replace bare `Icons.*` expressions with proper `Icon(imageVector, contentDescription)` composables in 2 files (8 instances total).

- **JobApplicationDetailScreen.kt**: Fix 4 `IconButton` content lambdas (Back, Notifications, Edit, Build icons) — wrap each bare `Icons.*` in `Icon(imageVector = Icons.*, contentDescription = "...")`.
- **JobApplicationsListScreen.kt**: Fix 3 `IconButton` content lambdas (Edit, Settings, Refresh) and 1 `FloatingActionButton` (Add) — same pattern.
- Verify the `contentDescription` values match the existing `semantics` modifiers on each button for accessibility consistency.