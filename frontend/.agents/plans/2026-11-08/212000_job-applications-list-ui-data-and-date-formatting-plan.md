# Implementation Plan: Job Applications List UI Edits, Missing Data Fallbacks, and `%s` Format String Resolution

**Status:** Completed  
**Date:** 2026-11-08  
**Target:** `ui/kmp`  
**Model:** claude-opus-4-6  
**Session:** 7119f97b-15f3-4a20-8cf2-b66898a0dd22  
**Depends On:** [`191000_android-date-display-and-style-fix-plan.md`](./191000_android-date-display-and-style-fix-plan.md)  

---

## 1. Executive Summary & Goals

Based on a visual and code audit of the Android `Job Applications` list screen screenshot (`uploaded_media_1794190421532.jpg`), this plan resolves four interconnected categories of defects:

1. **Literal `%s` and `%d` Format Specifiers Across Compose Resources:**
   - Every card in the screenshot renders `"Created %s"` literally instead of substituting the formatted `YYYY-MM-DD` date.
   - **Root Cause:** JetBrains Compose Multiplatform's `org.jetbrains.compose.resources` implementation (`replaceWithArgs`) uses `SimpleStringFormatRegex = Regex("""%(\d+)\$[ds]""")`, which **only** matches positional format specifiers (`%1$s`, `%1$d`, `%2$s`) and ignores unindexed `%s` and `%d`.
   - Note how `<string name="list_sort_button_label">%1$s (%2$s)</string>` in the toolbar renders properly as `"Date Created (Descending)"`, whereas `<string name="list_row_created">Created %s</string>` renders literally as `"Created %s"`.
   - Across `ui/kmp`, **52 string resources** in 7 `composeResources/values/strings.xml` files currently use unindexed `%s` or `%d` (including relative time strings like `%d days ago` in `core/widgets` and 26 strings in `job-application-details`).

2. **Company Logo Rendering Defects & Missing/Invalid Logo Handling (`CompanyLogoImage`):**
   - **Transparency Bleed-Through (`hireeazy` Rows 1–2, `eBay` Row 3):** [`CompanyLogoImage`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationRow.kt#L78-L106) renders the fallback initial `Text` (`"H"`, `"E"`) unconditionally inside the `Box` underneath `AsyncImage`. When a logo has a transparent background, the fallback letter bleeds right through the logo.
   - **Non-Square Banner / Sprite Images (`Affirm` Row 10, `eBay` Row 3):** `Affirm` resolves to a rectangular OpenGraph marketing banner (`"Flexible plans for every purchase - affirm"`), which `ContentScale.Crop` slices into the 90x90 square. `eBay` resolves to a small multi-icon sprite sheet.
   - **Missing Company Logo Metadata Plumbing:** Although [`Company`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/Company.kt) already models `logoUrl`, `logoWidth`, `logoHeight`, `logoShape` (`LogoShape.SQUARE` vs `LogoShape.RECTANGLE`), and `isSquare`, [`JobApplicationRow`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationRow.kt#L68-L72) still guesses URLs with `https://logos.hunter.io/$clean.com` and does not inspect image aspect ratio on load.

3. **Compact Card Layout, Truncation, Chevron Animation, & Toolbar Redundancy:**
   - **Role Title Clipping (`"Staff Android/"`, `"Staff Software"`, `"Staff Engineer -"`):** [`JobApplicationRow`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationRow.kt#L165-L231) reserves both a `90.dp` logo box at `START` and a `90.dp`-wide chevron box at `END` (`236.dp` of horizontal width consumed by non-text elements!) and omits `overflow = TextOverflow.Ellipsis` on `roleDesc` and `companyName`.
   - **Card Element Alignment vs. Specification ([`prompt.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/prompt.md)):** All 4 text items (`roleDesc`, `companyName`, `Created on ...`, `StatusBadge`) are stacked in a single left-aligned column instead of aligning the chevron compactly at the trailing edge and giving the title/company and created/status rows full horizontal breathing room.
   - **No-Op Chevron Rotation:** Line 226 has `.rotate(if (isExpanded) 0f else 0f)` instead of animating from `0f` to `90f`.
   - **Duplicate Navigation Actions in Secondary Toolbar:** [`JobApplicationsListScreen.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/JobApplicationsListScreen.kt#L234-L243) renders **Pending Edits** (pencil) and **Settings** (gear) buttons in the filter/sort toolbar row even though both destinations already exist in the bottom navigation bar, crowding the toolbar on compact screens and pushing the Settings icon off-screen.

4. **Exhaustive "No Data" / Missing Field Fallbacks:**
   - Defines deterministic, user-friendly behavior for every field when data is absent, null, blank, or `Instant.DISTANT_PAST` across deserialization (`RemoteJobApplicationsListDataSourceImpl`), UI modeling (`JobApplicationItemUiModel`), and Composable rendering (`JobApplicationRow` & `JobApplicationDetailsScreen`).

---

## 2. Architecture & Design Principles

### 2.1 Compose Multiplatform Positional Format Specifier Invariant
All parameterized `<string>` entries in `composeResources/values/strings.xml` across `ui/kmp` MUST use 1-based indexed placeholders (`%1$s`, `%1$d`, `%2$s`, etc.). A Konsist architecture test in [`KonsistArchitectureTest.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/apps/core/src/jvmTest/kotlin/dev/fnc314/tools/jobtracker/app/core/architecture/KonsistArchitectureTest.kt) will enforce that no `strings.xml` file contains unindexed `%s` or `%d`.

### 2.2 Stateful Logo Rendering & Aspect-Ratio Guard
In [`CompanyLogoImage`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationRow.kt#L78-L106):
- Track Coil's `AsyncImagePainter.State` via `onState`.
- Inspect the loaded painter's `intrinsicSize` on `AsyncImagePainter.State.Success`:
  - Reject non-square promotional banners (where width/height aspect ratio deviates significantly from `1:1`, e.g., ratio `< 0.8f` or `> 1.25f`, such as `Affirm`'s `1200x630` OpenGraph banner) or tiny sprite sheets, treating them as invalid logos so they fall back cleanly to the monogram initial avatar.
- **Mutually Exclusive Foreground/Fallback:**
  - When `imageState` is `Loading`, `Empty`, `Error`, or rejected by the aspect-ratio guard: render the `primaryContainer` background with the single uppercase initial letter (`companyName.trim().firstOrNull()?.uppercase() ?: "?"`).
  - When `imageState` is `Success` (and passes the square aspect-ratio guard): hide the fallback initial `Text` completely and render the `AsyncImage` using `ContentScale.Fit` over a clean surface container so transparent PNG/WebP logos (`hireeazy`, `Talentry`, `Polymarket`) never have text bleeding through behind them.

### 2.3 Exhaustive "No Data" Matrix

| Field / Datum | Data Source / Domain State When Missing | Current Broken Behavior | Target "No Data" Behavior |
| :--- | :--- | :--- | :--- |
| **`createdAt` (Created Date)** | `record.fields["createdAt"]` & `created_at` absent -> `Instant.DISTANT_PAST` | Ignores top-level `record.fields["date"]`; displays `"Created %s"` or `"Created DATA MISSING"` | 1. In [`RemoteJobApplicationsListDataSourceImpl.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/data/source/impl/RemoteJobApplicationsListDataSourceImpl.kt), fall back to `record.fields["date"]` when `createdAt`/`created_at` is missing.<br>2. In UI, if `createdDateDisplay == null` (`Instant.DISTANT_PAST`), display `"Created date unavailable"` (or `"Created: —"`) instead of `"Created DATA MISSING"`. |
| **`firstActivityAt` (Applied Date)** | `timeline.firstActivityAt` absent -> `Instant.DISTANT_PAST` | Falsely falls back to `createdAt` even when `status == DRAFTING` or `LEAD` | When status is pre-submission (`DRAFTING`, `LEAD`, `UNSPECIFIED`) and `firstActivityAt == Instant.DISTANT_PAST`, return `null` so the expanded grid shows `"Not applied"` (`Res.string.data_not_applied`) instead of copying `createdAt`. For post-submission statuses (`APPLIED`, `INTERVIEWING`, `OFFER`, `REJECTED`, etc.), fall back to `record.fields["date"]` / `createdAt`. |
| **`updatedAt` (Last Updated)** | `updatedAt == Instant.DISTANT_PAST` | Relative formatter or fallback shows `"%d days ago"` (literal `%d`) or `"DATA MISSING"` | Fall back to `lastActivityAt`, then `createdAt`; if all are `Instant.DISTANT_PAST`, display `"—"` (`Res.string.data_not_available`). With `%1$d` fixed in `core/widgets/strings.xml`, valid dates render `"3 days ago"`. |
| **`fitScore` (Fit Score)** | `fit.score` is `null` | Shows `"DATA MISSING"` in expanded grid | Return `null` from `fitScoreDisplay()`; display `"Not evaluated"` (`Res.string.data_not_evaluated`) in the expanded grid and omit `FitBadge` on wide rows. |
| **`roleTitle` (Job Title)** | `role` is `null` or blank | Shows `"No role"` without ellipsis | Fall back to localized `"Untitled Role"` (`Res.string.list_row_no_role`) with `maxLines = 1, overflow = TextOverflow.Ellipsis`. |
| **`companyName` (Company)** | `company` is empty or blank | `.take(1)` produces `""` (empty avatar box) and blank company line | Normalize blank company names to localized `"Unknown Company"` (`Res.string.list_row_unknown_company`) and render `"?"` in the monogram fallback avatar. |
| **Company Logo** | No logo URL, HTTP 404, offline, or non-square OpenGraph banner | Transparency bleed-through (`H` behind `hireeazy`), cropped marketing banner (`Affirm`) | Render monogram avatar (`first letter` or `"?"`) on `primaryContainer` when missing/404/banner; hide monogram completely once a valid square logo succeeds. |
| **`status` (Application Status)** | `TrackerStatus.Known.UNSPECIFIED` or empty raw status | Renders `"Unspecified"` badge | Render neutral `"Unspecified"` badge (`surfaceVariant` / `outline`). |

---

## 3. Proposed Changes & Implementation Steps

### Step 1: Fix All Unindexed `%s` and `%d` Format Specifiers Across 7 `strings.xml` Files

Update every single-argument `%s` -> `%1$s` and `%d` -> `%1$d` in:

1. [`ui/kmp/features/job-applications-list/ui/src/commonMain/composeResources/values/strings.xml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/composeResources/values/strings.xml):
   - `list_error_prefix`: `Error: %1$s`
   - `list_filter_cd`: `Active filter: %1$s`
   - `list_filter_label_status`: `Status: %1$s`
   - `list_filter_label_age`: `Age: %1$s`
   - `list_row_click_cd`: `View %1$s details`
   - `list_row_created`: `Created on %1$s` (aligning with [`prompt.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/prompt.md#L16))
   - Add clean missing-data strings:
     - `list_row_unknown_company`: `Unknown Company`
     - `data_not_available`: `—`
     - `data_not_applied`: `Not applied`
     - `data_not_evaluated`: `Not evaluated`
     - `list_row_created_unavailable`: `Created date unavailable`
2. [`ui/kmp/core/widgets/src/commonMain/composeResources/values/strings.xml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/core/widgets/src/commonMain/composeResources/values/strings.xml):
   - `notes_label_note`: `Note %1$d`
   - `notes_cd_delete`: `Delete Note %1$d`
   - `notes_cd_commit`: `Commit Note %1$d`
   - `time_minutes_ago`: `%1$d minutes ago`
   - `time_hours_ago`: `%1$d hours ago`
   - `time_days_ago`: `%1$d days ago`
   - `time_weeks_ago`: `%1$d weeks ago`
3. [`ui/kmp/features/job-application-details/ui/src/commonMain/composeResources/values/strings.xml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-application-details/ui/src/commonMain/composeResources/values/strings.xml):
   - Convert all 26 unindexed `%s` / `%d` strings (`detail_error_prefix`, `detail_prefix_location`, `detail_prefix_channel`, `detail_prefix_compensation`, `detail_fit_rating_label`, `detail_label_gaps`, `detail_prefix_job_url`, `detail_prefix_app_url`, `detail_company_profile_title`, `detail_company_logo_cd`, `detail_company_website`, `detail_company_industry`, `detail_company_aliases`, `dialog_fit_score_label`, `dialog_md_kind`, `dialog_md_path`, `dialog_event_detail_title`, `dialog_event_timestamp`, `dialog_event_actor`, `dialog_event_source`, `dialog_event_confidence`, `timeline_event_cd`, `artifacts_size_mb`, `artifacts_size_kb`, `artifacts_size_bytes`, `detail_last_updated`) to `%1$s` / `%1$d`.
4. [`ui/kmp/features/settings/ui/src/commonMain/composeResources/values/strings.xml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/settings/ui/src/commonMain/composeResources/values/strings.xml):
   - Convert `settings_label_theme` and `settings_cd_theme_color` to `%1$s`.
5. [`ui/kmp/features/tracker-edits/ui/src/commonMain/composeResources/values/strings.xml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/tracker-edits/ui/src/commonMain/composeResources/values/strings.xml):
   - Convert all 8 unindexed `%s` strings to `%1$s`.
6. [`ui/kmp/features/add-job-application/ui/src/commonMain/composeResources/values/strings.xml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/add-job-application/ui/src/commonMain/composeResources/values/strings.xml):
   - Convert all 4 unindexed `%s` strings to `%1$s`.
7. [`ui/kmp/features/sign-in/ui/src/commonMain/composeResources/values/strings.xml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/sign-in/ui/src/commonMain/composeResources/values/strings.xml):
   - Convert `sign_in_failed` to `%1$s`.

### Step 2: Improve Timestamp Fallback Deserialization & UI Model Missing-Data Semantics

1. **[`RemoteJobApplicationsListDataSourceImpl.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/data/source/impl/RemoteJobApplicationsListDataSourceImpl.kt):**
   - When parsing `createdAt`, check `record.fields["createdAt"] ?: record.fields["created_at"] ?: record.fields["date"]` so records stored with a top-level `date` (`YYYY-MM-DD`) never default to `Instant.DISTANT_PAST`.
   - When parsing `updatedAt`, check `record.fields["updatedAt"] ?: record.fields["updated_at"] ?: timelineMap["lastActivityAt"] ?: timelineMap["last_activity_at"]`.
2. **[`JobApplicationItemUiModel.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/JobApplicationItemUiModel.kt):**
   - Update `appliedDateDisplay(timeZone)`:
     - If `firstActivityAt != Instant.DISTANT_PAST`, format `firstActivityAt` as `YYYY-MM-DD`.
     - Otherwise, only fall back to `createdAt.dateOrNull(timeZone)` if `status` is an active/submitted application state (`APPLIED`, `INTERVIEWING`, `OFFER`, `REJECTED`, `WITHDRAWN`, `GHOSTED`, `ACCEPTED`, `DECLINED`). If `status` is `DRAFTING`, `LEAD`, or `UNSPECIFIED`, return `null` so the UI displays `"Not applied"` rather than duplicating the creation date as an applied date.
   - Add `val hasValidCompanyName: Boolean` and helper for clean monogram extraction (`companyName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"`).

### Step 3: Fix `CompanyLogoImage` Transparency Bleed-Through & Non-Square Banner Guard

In [`JobApplicationRow.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationRow.kt):
- Update `CompanyLogoImage` to track Coil state:
  - Observe `onState = { state -> ... }`.
  - When `state is AsyncImagePainter.State.Success`, inspect `state.painter.intrinsicSize`:
    - If both `width > 0f` and `height > 0f`, verify the aspect ratio (`val ratio = width / height; ratio in 0.8f..1.25f`) and minimum dimensions (`width >= 32f && height >= 32f`) so rectangular OpenGraph marketing banners (`Affirm`) and tiny sprite fragments (`eBay`) are rejected.
  - Render the fallback initial `Text` (`companyInitial`) on `MaterialTheme.colorScheme.primaryContainer` **only** when no valid square logo image is loaded (`!hasValidLogo`).
  - When `hasValidLogo` is true, hide the initial `Text` and render the `AsyncImage` with `ContentScale.Fit` on a clean surface background so transparent logos (`hireeazy`) never show the initial letter underneath.
- Apply the same state-aware fallback in [`CompanyProfileSection`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-application-details/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationdetails/ui/JobApplicationDetailsScreen.kt#L526-L534).

### Step 4: Fix Compact Card Layout, Text Truncation, Chevron Animation, & Toolbar Redundancy

1. **[`JobApplicationRow.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationRow.kt):**
   - Replace the oversized `width(90.dp)` trailing chevron `Box` with a compact trailing icon container (`40.dp` width, `90.dp` height, centered vertically) so the middle content area regains `50.dp` of horizontal width on compact screens.
   - Add `overflow = TextOverflow.Ellipsis` to `roleDesc`, `companyName`, and created date `Text` composables.
   - Animate the chevron rotation smoothly using `val chevronRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)` with `Icons.AutoMirrored.Filled.KeyboardArrowRight` and `.rotate(chevronRotation)`.
   - In the expanded 3-column metadata row (`Applied on`, `Updated`, `Fit Score`), render semantic missing-data strings (`"Not applied"`, `"—"`, `"Not evaluated"`) with subdued `onSurfaceVariant` styling when a value is `null`, rather than alarming all-caps `"DATA MISSING"`.
2. **[`JobApplicationsListScreen.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/JobApplicationsListScreen.kt):**
   - Remove the redundant `ClickEdits` (pencil) and `ClickSettings` (gear) `TooltipIconButton`s from the secondary filter/sort toolbar row on lines 234–243 (since both destinations are top-level items in the bottom/rail navigation bar), leaving `FilterMenu`, `SortMenu`, and `Refresh` cleanly aligned without horizontal clipping.

### Step 5: Add Unit & Konsist Architecture Tests

1. **[`KonsistArchitectureTest.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/apps/core/src/jvmTest/kotlin/dev/fnc314/tools/jobtracker/app/core/architecture/KonsistArchitectureTest.kt):**
   - Add a test scanning all `composeResources/values/strings.xml` files in `ui/kmp` to assert that no string resource contains unindexed `%s` or `%d` placeholders.
2. **[`JobApplicationItemUiModelTest.kt`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/features/job-applications-list/ui/src/commonTest/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/JobApplicationItemUiModelTest.kt):**
   - Add unit tests covering:
     - `appliedDateDisplay` returning `null` when `status == DRAFTING` and `firstActivityAt == Instant.DISTANT_PAST` (even when `createdAt` is valid).
     - `appliedDateDisplay` falling back to `createdAt` when `status == APPLIED` and `firstActivityAt == Instant.DISTANT_PAST`.
     - Missing `createdAt`, `updatedAt`, `fitScore`, blank `roleTitle`, and blank `companyName` fallbacks.

---

## 4. Verification Plan & Testing Checklist

- [x] Run `mise run :ui:kmp:test` from repository root and verify all unit tests and `KonsistArchitectureTest` pass.
- [x] Run `mise run :ui:kmp:detekt` from repository root and verify 0 static analysis violations across all modules.
- [x] Run `mise run :ui:kmp:android:assemble` from repository root and verify `BUILD SUCCESSFUL` with 0 warnings.

## 5. Bookkeeping
- [x] Plan persisted at `ui/kmp/.agents/plans/2026-11-08/212000_job-applications-list-ui-data-and-date-formatting-plan.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
