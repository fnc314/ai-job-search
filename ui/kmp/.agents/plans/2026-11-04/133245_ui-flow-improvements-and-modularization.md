# Architecture Plan: UI Flow Enhancements, Navigation & Modularization

**Status:** Completed  
**Date:** 2026-11-04  
**Target:** ui/kmp:features  
**Author:** Antigravity  

---

## Objective
Implement a suite of user experience, design system, and navigation upgrades to the KMP Job Tracker:
0. Architect & migrate UI flows from `shared` into dedicated `features/<Flow Name>/{domain,data,ui}` Gradle modules.
1. Replace `ColumnHeader`s with a layered drop-down/menu approach (Column selection + Ascending/Descending toggle, Mac-toolbar style).
2. Semantic timestamp formatting with strict precedence: `updatedAt` -> `createdAt` -> nothing.
   - `< 24 hours`: X hours ago
   - `< 7 days`: X days ago
   - `< 4 weeks`: X weeks and Y days ago
   - `< 2 months`: X months, Y weeks, and Z days ago
   - Thereafter: `YYYY-MM-DD (X months ago)`
3. Drop custom IconButton colors and add M3 `TooltipBox` hover/focus state labels.
4. Add leading icons to the FAB menu items.
5. Implement M3 `NavigationBar` (Bottom Navigation) with state-aware outlined/filled icons:
   - Primary: `Job Applications`
   - Secondary: `Pending Edits`
   - Tertiary: `Settings`
6. Surface actionable links for stored PDFs and Markdown artifacts (PDF -> default handler, Markdown -> default handler or in-app viewer via setting).
7. Remove the awkward expanding/collapsible card wrapper around Company Name in Details.
8. Populate and enrich the Events Timeline with custom event-type graphics, synthetic lifecycle fallback, and an interactive event detail inspection sheet/dialog.

---

## Plan Checklist

- [x] **Step 0: Scaffold Feature Modules (`features/job-applications-list` & `features/job-applications-details`) - *COMPLETE***
  - Scaffolded `features/job-applications-list/{domain,data,ui}` with multiplatform target configurations and Gradle accessors.
  - Scaffolded `features/job-applications-details/{domain,data,ui}` with multiplatform target configurations and Gradle accessors.
- [x] **Step 1: Semantic Timestamp Formatter with Precedence (`updatedAt` -> `createdAt`) - *COMPLETE***
  - Created `SemanticTimestampFormatter.kt` implementing the exact tiered duration rules.
  - Integrated into `JobApplicationRow` and `JobApplicationDetailScreen` with strict `updatedAt` -> `createdAt` -> nothing precedence.
- [x] **Step 2: Layered Mac-Toolbar Style Sort Dropdown (Dropping ColumnHeader) - *COMPLETE***
  - Created `SortMenu.kt` featuring a layered M3 dropdown (Layer 1: Column selection, Layer 2: Direction choice).
  - Dropped `ColumnHeader` and added `setSort` in `JobApplicationsListViewModel`.
- [x] **Step 3: M3 NavigationBar (Bottom Navigation with Filled/Outlined Icons) - *COMPLETE***
  - Added M3 `NavigationBar` in `JobApplicationsListScreen`: Applications, Pending Edits, and Settings.
  - State-aware icon rendering: Filled icons when active, outlined icons when inactive.
- [x] **Step 4: Standardize IconButtons with M3 Tooltips & Add FAB Menu Icons - *COMPLETE***
  - Created reusable `TooltipIconButton.kt` in `:core:design-system`.
  - Removed custom colors (`IconButtonDefaults.iconButtonVibrantColors`, `outlinedIconButtonColors`) across all screens.
  - Added leading icons to all FAB menu items (`Icons.Default.Add`, `Icons.Default.Person`, `Icons.Default.Edit`).
- [x] **Step 5: Details Page Cleanup: Remove Collapsible Company Wrapper & Enrich Artifact Links - *COMPLETE***
  - Removed the accordion wrapper around company profile in `JobApplicationDetailScreen`.
  - Added direct links in `ArtifactsList` for PDFs (default handler) and Markdown (default handler or in-app viewer).
  - Added `renderMarkdownInternally` preference to `AppSettings` and `SettingsDialog`.
  - Built in-app `MarkdownPreviewDialog` in `DetailDialogs.kt`.
- [x] **Step 6: Rich Graphic Events Timeline & Event Details Inspection - *COMPLETE***
  - Embedded `val icon: ImageVector` and `val displayName: String` directly into the canonical `TrackerEventType` enum definition in `Enums.kt`.
  - Completely eradicated static converter/helper methods (e.g. `getEventIcon()`) in favor of direct property access (`event.type.icon`, `event.type.displayName`).
  - Corrected synthetic lifecycle events to use real canonical enum members (`TrackerEventType.APPLY`, `TrackerEventType.CORRECT_STATUS`).
  - Upgraded `EventsTimeline` with circular icon nodes tailored to each `TrackerEventType`.
  - Added automatic fallback to synthesize lifecycle events from `createdAt`, status, and `updatedAt` when subcollection events are empty.
  - Made timeline nodes interactive, opening `EventDetailDialog` to inspect actor, source, confidence, status transition, and notes.

- [x] **Step 7: Tokenized Dimensions, Strict ColorScheme & Core-Only Material Icons Audit - *COMPLETE***
  - Defined centralized `Dimensions.kt` token set (`IconSizes` and `ComponentSizes`) in `:core:design-system` provided via `CompositionLocalProvider`.
  - Audited every Compose file across `shared`, `core/widgets`, and `features`: eliminated all raw `Int.dp` and floating dimension calls in favor of `MaterialTheme.spacing.*`, `MaterialTheme.iconSizes.*`, and `MaterialTheme.componentSizes.*`.
  - Eliminated hardcoded `Color(...)` calls in UI components (e.g. `StatusBadge.kt`), ensuring all colors derive strictly from semantic slots in `MaterialTheme.colorScheme`.
  - Audited all Material icons across the project to strictly use `material-icons-core:1.7.3`, replacing all non-existent extended icons (`OpenInNew`, `Description`, `PictureAsPdf`, `Visibility`, `Sort`, `ArrowUpward`, `ArrowDownward`, `Event`, `Notes`) with core equivalents.
