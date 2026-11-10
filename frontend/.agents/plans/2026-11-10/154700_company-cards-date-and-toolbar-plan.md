# Implementation Plan: Company Cards Date Display, Toolbar Restructuring & Company Filters

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `frontend` (`frontend/features/companies-list/*`, `frontend/apps/core/*`, `frontend/core/data/*`, `frontend/features/settings/*`)  

---

## Goal Description
Enhance the Companies catalog view and employer card widgets:
1. **`CompanyCard` Tweaks (`CompactCompanyCard` and `WideCompanyCard`)**:
   - Remove the `FitBadge` / Fit score information from the company card headers.
   - Retain the `Submitted: X` and `Total: Y` counts.
   - Beneath them (or moving horizontally to the right as space permits), display `Last Application Submitted YYYY-MM-DD`.
   - **Zero applications suppression**: If a company has **no submitted applications** (`submittedApplications == 0` or no submission date), **suppress this field entirely** (do not show `"Last Application Submitted"` or `"N/A"`).
   - In narrower viewport constraints where this information moves horizontally, apply a vertical stack so `YYYY-MM-DD` is displayed on a separate new line beneath `Last Application Submitted`.
   - Ensure all user-facing strings are backed by `Res.string` declarations.
2. **Top App Bar Restructuring**:
   - In `AppScaffold.kt`, drop the `Search` (`Icons.Filled.Search`) and `Calendar` (`Icons.Filled.DateRange`) action widgets from the top-most `CenterAlignedTopAppBar`.
   - Move the `Refresh` (`Icons.Default.Refresh`) icon action button into the top-most `CenterAlignedTopAppBar` actions.
3. **Settings Color & Theme Reactivity Enforcement**:
   - Ensure the Settings experience (`SettingsScreen`, `SettingsViewModel`, `AppSettingsRepository`) propagates and enforces all UI components to immediately adopt newly selected seed colors and theme modes (Light/Dark/System) across the entire application hierarchy.
4. **Companies Page Secondary Search & Filter Bar (Following `features/job-applications-list`)**:
   - Ensure the `OutlinedTextField` search bar does not wrap or truncate placeholder text (`singleLine = true`).
   - In the secondary toolbar row beneath the top app bar:
     - Implement a dropdown `CompanyFilterMenu` matching the design and UX pattern of [`FilterMenu.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/FilterMenu.kt) in `features/job-applications-list`.
     - Filter options:
       - `All Companies` (clear filter)
       - `Filter by company with open applications`
       - `Filter by company with ongoing interview`
       - `Filter by company with open offer`
     - Beside the filter menu, place the `SortMenu` (sort key picker).
     - Remove the local refresh button from this row (as it is moved to the top-most app bar).

---

## User Review Required

> [!IMPORTANT]
> ### 1. "Last Application Submitted YYYY-MM-DD" Suppression Invariant
> - When `submittedApplications == 0` or `lastApplicationSubmittedDate == null`, the `"Last Application Submitted"` field is completely omitted from both `CompactCompanyCard` and `WideCompanyCard`.
> - When `submittedApplications > 0` and a date exists, it renders either inline (`Last Application Submitted YYYY-MM-DD`) or vertically stacked when constrained (`Last Application Submitted` on line 1, `YYYY-MM-DD` on line 2).

> [!IMPORTANT]
> ### 2. Dropdown Filter Menu UX Pattern Parity with `features/job-applications-list`
> - `CompanyFilterMenu` will use `FilledTonalButton` with `Icons.AutoMirrored.Filled.List` (or `Icons.Filled.FilterList`), displaying the active filter label, chevron dropdown icon, and an anchored `DropdownMenu` with checkmarks on the active selection.

---

## Proposed Changes

### 1. `frontend/core/data` & Domain Model Updates

#### [MODIFY] [`CompanyMetrics.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/CompanyMetrics.kt)
- Add optional `lastApplicationSubmittedAt: Instant? = null` property to `CompanyMetrics`.
- Parse `lastApplicationSubmittedAt` (or `last_application_submitted_at`) in `CompanyMetrics.fromMap()`.

---

### 2. `frontend/features/companies-list` Domain & Use Cases

#### [MODIFY] [`FilterCompaniesUseCase.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/companies-list/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/companieslist/domain/FilterCompaniesUseCase.kt)
- Define `CompanyStatusFilter`:
  ```kotlin
  public enum class CompanyStatusFilter {
      ALL,
      OPEN_APPLICATIONS,
      ONGOING_INTERVIEW,
      OPEN_OFFER,
  }
  ```
- Support combined filtering: text query matching (name, industry, location, alias) + status filter:
  - `OPEN_APPLICATIONS`: `counts.submitted > 0 || counts.interview > 0`
  - `ONGOING_INTERVIEW`: `counts.interview > 0`
  - `OPEN_OFFER`: `counts.offer > 0`

---

### 3. `frontend/features/companies-list/ui` Presentation & Card Tweaks

#### [MODIFY] [`strings.xml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/companies-list/ui/src/commonMain/composeResources/values/strings.xml)
- Add string resources:
  - `company_card_last_submitted_label`: `"Last Application Submitted"`
  - `companies_filter_all`: `"All Companies"`
  - `companies_filter_open_apps`: `"Open Applications"`
  - `companies_filter_ongoing_interview`: `"Ongoing Interview"`
  - `companies_filter_open_offer`: `"Open Offer"`
  - `companies_filter_menu_tooltip`: `"Filter companies by status"`
  - `companies_filter_cd`: `"Filter companies: %1$s"`

#### [MODIFY] [`CompanyCardUiModel.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/companies-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/companieslist/ui/CompanyCardUiModel.kt)
- Add `lastApplicationSubmittedDate: String?` (formatted as `YYYY-MM-DD` or `null`).
- Derive date: if `submittedApplications > 0`, extract `YYYY-MM-DD` from `lastApplicationSubmittedAt` or `lastCalculatedAt`; if `submittedApplications == 0`, set to `null`.
- Remove `averageFitScore` from card header requirements.

#### [MODIFY] [`CompanyCard.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/companies-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/companieslist/ui/component/CompanyCard.kt)
- **`CompactCompanyCard`**:
  - Remove `FitBadge`.
  - Retain `Submitted: X · Total: Y`.
  - If `model.lastApplicationSubmittedDate != null`:
    - Display a Column containing `Text(stringResource(Res.string.company_card_last_submitted_label))` and `Text(model.lastApplicationSubmittedDate)` on a new line.
- **`WideCompanyCard`**:
  - Remove `FitBadge` from trailing column.
  - Display `Submitted: X · Total: Y`.
  - If `model.lastApplicationSubmittedDate != null`:
    - Display `Last Application Submitted` and `YYYY-MM-DD` (supporting responsive horizontal wrap / vertical stack based on available space).

#### [NEW] [`CompanyFilterMenu.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/companies-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/companieslist/ui/component/CompanyFilterMenu.kt)
- Modeled directly on [`FilterMenu.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/FilterMenu.kt):
  - `FilledTonalButton` with active filter label and `Icons.Filled.KeyboardArrowDown`.
  - `DropdownMenu` showing options: All Companies, Open Applications, Ongoing Interview, Open Offer.
  - Active checkmark (`Icons.Filled.Check`) indicating current filter.

#### [MODIFY] [`CompaniesListScreen.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/companies-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/companieslist/ui/CompaniesListScreen.kt)
- Fix search bar placeholder wrapping: set `singleLine = true`, ensure proper width constraint and style.
- Secondary toolbar layout (mirroring `JobApplicationsListScreen.kt`):
  - Row 1: `CompanyFilterMenu` on the start side, `SortMenu` on the end side.
  - Row 2 / search bar integration: cleanly laid out so search bar has ample horizontal room.
  - Drop the local refresh icon button from this row.

#### [MODIFY] [`CompaniesListViewModel.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/features/companies-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/companieslist/ui/CompaniesListViewModel.kt), `CompaniesListUiState.kt`, `CompaniesListUiIntent.kt`
- Add `statusFilter: CompanyStatusFilter` to `CompaniesListUiState.Content`.
- Handle `CompaniesListUiIntent.ChangeStatusFilter(filter: CompanyStatusFilter)`.

---

### 4. `frontend/apps/core` Top Bar & Settings Theme Reactivity

#### [MODIFY] [`AppScaffold.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/ui/AppScaffold.kt)
- Remove `Search` (`Icons.Filled.Search`) and `Calendar` (`Icons.Filled.DateRange`) `IconButton`s from `CenterAlignedTopAppBar` actions.
- Add `onRefreshClick: () -> Unit = {}` parameter.
- Add `Refresh` (`Icons.Default.Refresh`) `IconButton` with `Res.string.app_action_refresh` in `CenterAlignedTopAppBar` actions.

#### [MODIFY] [`App.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/App.kt)
- Ensure `settingsRepository.settings` is observed reactively by `JobTrackerTheme` so changing seed colors or theme mode immediately recomposes the UI tree with the new palette.
- Wire `onRefreshClick` to trigger active screen refresh or broadcast refresh.

---

## Verification Plan

### Automated Tests
Run full Gradle test suites across frontend and companies list:
```bash
./gradlew :frontend:features-companies-list-ui:jvmTest :frontend:features-companies-list-domain:jvmTest :frontend:apps-core:jvmTest
```

### Manual Verification
1. Run desktop application:
   ```bash
   ./gradlew :frontend:apps-desktop:run
   ```
2. Navigate to **Companies** page:
   - Verify top app bar displays centered title and the **Refresh** icon button (no search or calendar icons).
   - Verify the search bar placeholder does not wrap.
   - Verify secondary toolbar displays `CompanyFilterMenu` and `SortMenu`.
   - Test filtering:
     - `Filter by company with open applications`
     - `Filter by company with ongoing interview`
     - `Filter by company with open offer`
     - `All Companies`
   - Inspect Company Cards (both wide and compact):
     - Verify no `FitBadge` appears.
     - Verify `Submitted` and `Total` application counts remain visible.
     - Verify companies with **0 submitted applications** suppress the `Last Application Submitted` field completely.
     - Verify companies with submitted applications display `Last Application Submitted` with `YYYY-MM-DD` on a separate line beneath it when horizontally constrained.
3. Settings Color Verification:
   - Navigate to Settings, pick a new seed color / toggle dark theme.
   - Verify the whole UI immediately adopts the new color scheme.
