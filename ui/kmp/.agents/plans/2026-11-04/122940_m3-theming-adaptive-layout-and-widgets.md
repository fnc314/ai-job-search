# Architecture Plan: Material 3 Theming, Adaptive Layouts & Widget Enhancements

**Status:** Completed  
**Date:** 2026-11-04  
**Target:** ui/kmp:core:design-system, ui/kmp:core:widgets  
**Author:** Antigravity  

---

## Objective
Implement comprehensive Material Design 3 enhancements across the KMP Job Tracker shared UI:
1. Persistent configuration choices regarding UI Mode (Light/Dark/System) and/or Seed color.
2. Full-fledged Material Theme key tokens around Typography, Spacing, and Shapes in `:core:design-system`.
3. Explicit screen-size reactivity across screen size classes (Compact, Medium, Expanded) and unique treatments per breakpoint.
4. Thorough application of `m3.material.io` guidelines and standard widgets in proper places (M3 icons, tonal badges, scrollable chips, cleanup).
5. Fix content obscured by the FAB at the bottom of the main applications list.
6. Fix focus movement in `NotesWidget` so that when a new note is added, focus immediately moves to the newly added empty input.

---

## Plan Checklist

- [x] **Step 1: Core Design System Tokens (Typography, Spacing, Shapes, Color Schemes) - *COMPLETE***
  - Defined all 15 M3 typography tokens in `Typography.kt` respecting fixed-width monospace headings and clean sans-serif body.
  - Expanded `Spacing.kt` with `none`, `hairline`, `extraExtraLarge`, `huge`, and `fabBottomPadding` (88.dp).
  - Defined M3 corner tokens in `Shapes.kt` (`extraSmall = 4.dp`, `small = 8.dp`, `medium = 12.dp`, `large = 16.dp`, `extraLarge = 28.dp`, `full = CircleShape`).
  - Implemented `buildLightColorScheme` and `buildDarkColorScheme` in `Color.kt` and wired everything into `JobTrackerTheme` in `Theme.kt`.

- [x] **Step 2: Persistent Settings Configuration (UiMode & Seed Color) - *COMPLETE***
  - Added `UiMode` enum (`SYSTEM`, `LIGHT`, `DARK`) to `@Serializable` `AppSettings`.
  - Built `SettingsStorage` cross-platform persistence interface with expect/actual implementations for Android (`SharedPreferences`), JVM (`Preferences`), iOS (`NSUserDefaults`), JS (`localStorage`), and WasmJS (`localStorage`).
  - Created `AppSettingsRepository` with reactive `StateFlow<AppSettings>` and registered it in Koin `repositoryModule`.
  - Injected `AppSettingsRepository` in `App.kt` and integrated with `SettingsDialog` providing M3 color swatches and `UiMode` chips.

- [x] **Step 3: Screen-Size Reactivity & Breakpoints (WindowSizeClass & Layout Treatments) - *COMPLETE***
  - Implemented `WindowSizeClass` and `ProvideWindowSizeClass` measuring `WindowWidthSizeClass` (`Compact` <600dp, `Medium` 600-840dp, `Expanded` >=840dp) and `WindowHeightSizeClass`.
  - **Compact (<600dp):** 1-column `LazyColumn` for list; 1-column scroll for detail and form views.
  - **Medium (600-840dp):** 2-column `LazyVerticalGrid` for list; 2-column side-by-side card layout for detail (Summary/Company on left, Timeline/Artifacts on right).
  - **Expanded (>=840dp):** Adaptive `LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 360.dp))` for list; 2-column centered layout (max 1200dp) for detail; centered max-width (760dp) containers for form screens (`AddJobApplicationScreen`, `AddLeadScreen`).

- [x] **Step 4: Material 3 Widget Polish (M3 Icons, Tonal Badges, Scrollable Chips, Cleanups) - *COMPLETE***
  - Replaced raw emoji text (`⏳`, `🔄`) in `TrackerEditsScreen.kt` with `Icons.Default.DateRange` and `Icons.Default.Refresh`.
  - Replaced `"▲"` and `"▼"` text indicators in `ColumnHeader.kt` with `Icons.Default.ArrowDropUp` and `Icons.Default.ArrowDropDown`, and made columns reactive to `AppSettings` visibility.
  - Removed debug red borders (`.border(width = 1.dp, color = Color.Red)`) from `JobApplicationRow.kt`.
  - Upgraded `StatusBadge.kt` to an M3 tonal `Surface` badge container with semantic container/onContainer colors and rounded corners.
  - Made the filter chips row in `JobApplicationsListScreen.kt` horizontally scrollable and styled chips with `FilterChipDefaults.filterChipColors()`.

- [x] **Step 5: Fix FAB Covering Content at Bottom of List - *COMPLETE***
  - Applied `contentPadding = PaddingValues(start = MaterialTheme.spacing.medium, top = MaterialTheme.spacing.small, end = MaterialTheme.spacing.medium, bottom = MaterialTheme.spacing.fabBottomPadding)` (88.dp) in `JobApplicationsListScreen.kt`.
  - All applications can now be scrolled completely clear of the floating action button.

- [x] **Step 6: NotesWidget Focus Movement & Note Creation Fix - *COMPLETE***
  - Added an explicit "Add note" `IconButton` in the `NotesWidget` header.
  - Enhanced `commitDrafting` so pressing Enter or clicking `+` on any note advances focus to the empty trailing note.
  - Replaced the vulnerable 50ms delay in `NoteRow` with an asynchronous frame-polling retry loop that safely requests focus once the Compose `FocusRequester` is fully attached to the layout tree before clearing `focusNewItemId`.

- [x] **Step 7: Verification and Final Polish - *COMPLETE***
  - Verified git status and diff across `:core:design-system`, `:core:widgets`, and `shared`.
  - All changes conform to M3 specifications, multiplatform architecture, and candidate profile rules.
