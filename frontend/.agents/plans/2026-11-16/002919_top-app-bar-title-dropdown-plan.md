# Implementation Plan: TopCenterAppBar Title Dropdown Summary Widget

**Status:** Completed  
**Date:** 2026-11-16  
**Target:** `frontend/apps/core`, `frontend/core/navigation`, `frontend/core/widgets`, `frontend/features`  
**Schema Authority:** `records/proto/tracker/v1`  
**Depends On:** `LocalTopBarActionsSlot`, `AppScaffold.kt`, M3 `CenterAlignedTopAppBar`  

---

## Goal Description
Introduce an interactive title dropdown widget within the `title` slot of the Material 3 `CenterAlignedTopAppBar` rendered by `AppScaffold.kt` (and populated/contributed to by individual screens across `@frontend/features`).

The widget will:
1. **Preserve Current Text:** Retain the screen's existing title text (e.g. `"Job Applications"`, company name, role/detail title).
2. **Animated Chevron:** Display a downward chevron (`Icons.Default.KeyboardArrowDown` or `Icons.Default.ArrowDropDown`) when closed, and smoothly rotate 180° upward when open via Compose `animateFloatAsState`.
3. **Show/Collapse Screen Summaries:**
   - **Job Applications (List):** Summary popup showing totals for **Applied**, **Rejected**, **Interviewing**, and **Draft**.
   - **Job Application Details:** Summary popup showing the **Last event which changed the status** (actor, timestamp, status transition, and note).
   - **Companies (List):** Summary popup showing **Total companies listed** and an **industry/sector breakdown** (counts per sector).
   - **Company Details:** Summary popup showing **Total open apps**, **Industry**, and **Most recent event** (timestamp & event type).
4. **Underlying M3 Widget Invariant:** Keep `CenterAlignedTopAppBar` unchanged in its positioning, colors, window insets, navigation icon, and actions slots.

---

## Architecture & Data Flow

```
+-----------------------------------------------------------------------------------+
| AppScaffold (CenterAlignedTopAppBar)                                              |
|                                                                                   |
|  [NavIcon]      [ TitleText + Animated Chevron ^/v ]                [Actions...]  |
|                                |                                                  |
|                        (Click opens DropdownMenu)                                 |
|                                |                                                  |
|                   +----------------------------+                                  |
|                   | Screen-Specific Summary    |                                  |
|                   | (Job Apps / Details / ...) |                                  |
|                   +----------------------------+                                  |
+-----------------------------------------------------------------------------------+
                                   ▲
                                   │ CompositionLocal: LocalTopBarTitleSummarySlot
                                   │ (provided by AppScaffold, populated by active screen)
+-----------------------------------------------------------------------------------+
| Feature Screen (JobApplicationsListScreen / Details / Companies / CompanyDetails)  |
| -> Populates summary data via LocalTopBarTitleSummarySlot.current.summary.value   |
+-----------------------------------------------------------------------------------+
```

### 1. CompositionLocal Communication Pattern
Just as `LocalTopBarActionsSlot` enables child screens to contribute trailing toolbar buttons to the scaffold's `TopAppBar`, we will introduce:
- `TopBarTitleSummarySlot`: A state holder in `frontend/core/navigation/TopBarAction.kt`:
  ```kotlin
  public class TopBarTitleSummarySlot {
      public val summary: MutableState<(@Composable (onDismiss: () -> Unit) -> Unit)?> =
          mutableStateOf(null)
  }

  public val LocalTopBarTitleSummarySlot: ProvidableCompositionLocal<TopBarTitleSummarySlot> =
      staticCompositionLocalOf { TopBarTitleSummarySlot() }
  ```
- If `summary.value != null`, the `title` composable in `AppScaffold` wraps the title text in a clickable row with the animated chevron and attaches an M3 `DropdownMenu`.
- If `summary.value == null` (e.g. Settings, Add Application, or while Loading), it renders the plain `Text(titleText)` as it does today.

---

## Screen Summary Specifications

### 1. Job Applications List Screen
- **Content:**
  - Status counts calculated from `JobApplicationsListUiState.Content.applications`:
    - **Applied:** count of apps where status is `APPLIED` or `SUBMITTED`
    - **Interviewing:** count of apps with `INTERVIEWING` or `RECRUITER_SCREENED`
    - **Rejected:** count of apps with `REJECTED` or `ARCHIVED`
    - **Draft:** count of apps with `DRAFT` or `SAVED`
  - Rendered in a clean M3 surface grid / chips showing status badges and counts.

### 2. Job Application Details Screen
- **Content:**
  - Extracted from `JobApplicationDetailUiModel.timelineEvents`:
    - Finds the latest event where `statusTo` is specified (`event.statusTo != TrackerStatus.UNSPECIFIED`).
    - Displays:
      - Status Transition: e.g. `APPLIED → INTERVIEWING`
      - Event Date / Relative time: e.g. `Nov 12, 2026 (4d ago)`
      - Note/Actor: e.g. `Interview invitation received via email`
    - If no status-changing event exists, displays `"Initial submission"`.

### 3. Companies List Screen
- **Content:**
  - Calculated from `CompaniesListUiState.Content.companies`:
    - **Total Companies:** total count
    - **Industry Breakdown:** grouped by `company.industry` (or sector), displaying top sectors and company counts (e.g., `FinTech: 12`, `Enterprise Tech: 8`, `HealthTech: 4`).

### 4. Company Details Screen
- **Content:**
  - Calculated from `CompanyDetailsUiModel`:
    - **Total Open Apps:** count of applications not in `REJECTED` or `ARCHIVED` (e.g. `draftCount + submittedCount + interviewingCount`).
    - **Industry:** display company industry/sector string (e.g. `"Financial Services / FinTech"`).
    - **Most Recent Event:** latest event from the company's applications timeline (event type, date, role).

---

## Proposed Changes

### Component 1: Navigation State & Core Widgets (`core-navigation`, `core-widgets`)

#### [MODIFY] `frontend/core/navigation/src/commonMain/kotlin/.../navigation/TopBarAction.kt`
- Add `TopBarTitleSummarySlot` class and `LocalTopBarTitleSummarySlot` composition local.

#### [NEW] `frontend/core/widgets/src/commonMain/kotlin/.../widgets/TopBarTitleDropdown.kt`
- Reusable M3 title dropdown composable:
  ```kotlin
  @Composable
  public fun TopBarTitleDropdown(
      title: String,
      isExpanded: Boolean,
      onToggle: () -> Unit,
      onDismiss: () -> Unit,
      modifier: Modifier = Modifier,
      dropdownContent: @Composable () -> Unit,
  )
  ```
  - Animate rotation of `Icons.Default.KeyboardArrowDown` between `0f` (closed) and `180f` (open) via `animateFloatAsState`.
  - Display M3 `DropdownMenu` anchored to the title row.

---

### Component 2: Application Scaffold (`apps-core`)

#### [MODIFY] `frontend/apps/core/src/commonMain/kotlin/.../app/core/ui/AppScaffold.kt`
- Create and provide `titleSummarySlot = remember { TopBarTitleSummarySlot() }` via `CompositionLocalProvider(LocalTopBarTitleSummarySlot provides titleSummarySlot)`.
- Reset `titleSummarySlot.summary.value = null` on destination transition.
- In `CenterAlignedTopAppBar` `title` lambda:
  - If `titleSummarySlot.summary.value != null`: render `TopBarTitleDropdown` with title text, chevron animation, and summary popup.
  - Else: render standard `Text(text = titleText, style = MaterialTheme.typography.titleLarge)`.

---

### Component 3: Feature Screens Integration (`frontend/features/*`)

#### [MODIFY] `frontend/features/job-applications-list/ui/.../JobApplicationsListScreen.kt`
- When in `JobApplicationsListUiState.Content`, publish summary composable to `LocalTopBarTitleSummarySlot.current.summary.value` showing totals for Applied, Interviewing, Rejected, Draft.

#### [MODIFY] `frontend/features/job-application-details/ui/.../JobApplicationDetailsScreen.kt`
- When in `JobApplicationDetailUiState.Content`, publish summary composable displaying the last status-changing event (transition, date, note).

#### [MODIFY] `frontend/features/companies-list/ui/.../CompaniesListScreen.kt`
- When in `CompaniesListUiState.Content`, publish summary composable displaying total companies count and industry breakdown.

#### [MODIFY] `frontend/features/company-details/ui/.../CompanyDetailsScreen.kt`
- When in `CompanyDetailsUiState.Content`, publish summary composable displaying open applications count, industry, and most recent event.

---

## Verification Plan

### Automated Checks
- `mise run dev:guards` — verify interface-naming and code standards.
- `mise run frontend:test` — run existing UI and Konsist architectural tests.
- `mise run frontend:android:assemble` — verify Android compilation with the new TopAppBar components.
- `mise run dev:detekt` — ensure static analysis passes.

### Manual Verification
1. **Desktop / Web / Android:**
   - Navigate to **Job Applications**: click title `"Job Applications"` -> confirm chevron rotates up to 180°, popup displays counts for Applied, Interviewing, Rejected, Draft.
   - Navigate to an **Application Detail**: click title -> confirm chevron rotates, popup displays latest status-changing event details.
   - Navigate to **Companies**: click title `"Companies"` -> confirm chevron rotates, popup displays company total and breakdown by sector/industry.
   - Navigate to **Company Details**: click title -> confirm chevron rotates, popup displays open apps count, industry, and recent activity.
   - Tap outside -> confirm popup dismisses and chevron animates smoothly back to 0° (downward).
