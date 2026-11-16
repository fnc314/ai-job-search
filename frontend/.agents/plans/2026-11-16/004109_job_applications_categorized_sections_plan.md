# Implementation Plan: Job Applications List Categorized Sections & Sticky Headers

**Status:** Completed  
**Date:** 2026-11-16  
**Target:** `frontend/features/job-applications-list/ui`, `frontend/features/job-application-details/ui`, `frontend/core/data`, `frontend/core/widgets`  
**Schema Authority:** `records/proto/tracker/v1`  
**Depends On:** Compose `LazyColumn` `stickyHeader` API, `rememberLazyListState`, `TopLevelCard`, `JobApplicationCard`, `composeResources`, `MaterialTheme.spacing.fabBottomPadding`  

---

## Goal Description
Break down the master Job Applications list (`JobApplicationsListScreen.kt`) into 4 prioritized, distinctive categories:
1. **INTERVIEWING** (Active interviews: `TrackerStatus.Known.INTERVIEW`, `OFFER_EXPECTED`, `HIRED`) — prominent, bright, high-priority, attention-grabbing.
2. **IN DRAFT/DRAFTING** (Incomplete applications: `TrackerStatus.Known.DRAFTING`, `LEAD`) — action-oriented, urgent/pestering reminder to finalize and submit.
3. **APPLIED** (Submitted applications awaiting response: `TrackerStatus.Known.APPLIED`) — standard/subtle, clean, stable.
4. **REJECTED** (Closed/terminal outcomes: `TrackerStatus.Known.REJECTED`, `NO_RESPONSE`, `OFFER_DECLINED`, `APPLICATION_WITHDRAWN`) — subdued, grayed-out, disabled appearance.

Each section in `LazyColumn` (Compact viewport) has a header that sticks immediately below the `CenterAlignedTopAppBar` while scrolling through that section until the subsequent section header arrives. When stuck (or tapped), the sticky section header provides a quick toggle/jump menu to scroll directly to any other section. In larger viewports (`LazyVerticalGrid`), section headers render as standard full-width headers (`GridItemSpan(maxLineSpan)`) preserving standard Material multi-column grid layout treatments.

Additionally, this plan shores up scroll position stability and introduces:
- A smooth animated **"Jump to Top" Floating Action Button** when the user scrolls down past the initial viewport.
- Audit & enforcement of **FAB clearance bottom padding (`MaterialTheme.spacing.fabBottomPadding`)** across screens with FABs (specifically fixing `JobApplicationDetailsScreen.kt` where the timeline/events list currently gets cut off by the edit FAB).

---

## Architecture & Visual Treatments

### 1. Embed Section & Visual Treatment in Existing Data Models (No Arbitrary Enums)
Rather than inventing disconnected enums, we enrich `TrackerStatus` in `frontend/core/data` (which already defines `containerColor()`, `contentColor()`, `isTerminal`, `isActive`):

```kotlin
public sealed interface TrackerStatus : WireContract {
    ...
    /** Resolves the category section group for list organization: 1=Interviewing, 2=Drafting, 3=Applied, 4=Rejected. */
    public val sectionOrder: Int

    /** Resolves the surface variant for the application card based on lifecycle state. */
    public val cardSurfaceVariant: TopLevelCardSurfaceVariant

    /** Resolves whether the card should render with subdued/disabled alpha and styling. */
    public val isSubduedCard: Boolean

    /** Resolves whether the card should present an action-required pestering flair. */
    public val isActionRequiredCard: Boolean
}
```

In `TrackerStatus.Known`:
- `INTERVIEW`, `OFFER_EXPECTED`, `HIRED`:
  - `sectionOrder = 1`
  - `cardSurfaceVariant = TopLevelCardSurfaceVariant.Highlighted`
  - `isSubduedCard = false`
  - `isActionRequiredCard = false`
- `LEAD`, `DRAFTING`:
  - `sectionOrder = 2`
  - `cardSurfaceVariant = TopLevelCardSurfaceVariant.FilledTonal`
  - `isSubduedCard = false`
  - `isActionRequiredCard = true` (triggers visual reminder badge/pester styling like *"Draft — Needs Submission"*)
- `APPLIED`:
  - `sectionOrder = 3`
  - `cardSurfaceVariant = TopLevelCardSurfaceVariant.FilledTonal`
  - `isSubduedCard = false`
  - `isActionRequiredCard = false`
- `REJECTED`, `NO_RESPONSE`, `OFFER_DECLINED`, `APPLICATION_WITHDRAWN`:
  - `sectionOrder = 4`
  - `cardSurfaceVariant = TopLevelCardSurfaceVariant.Surface`
  - `isSubduedCard = true` (card renders with dimmed alpha / grayed-out container and text, presenting as low priority / disabled)

### 2. Localization via String Resources
All user-visible titles and strings are defined in `composeResources/values/strings.xml`:
- `Res.string.list_section_interviewing` → `"Interviewing"`
- `Res.string.list_section_drafting` → `"In Draft"`
- `Res.string.list_section_applied` → `"Applied"`
- `Res.string.list_section_rejected` → `"Rejected & Closed"`
- `Res.string.list_section_jump_tooltip` → `"Jump to section"`
- `Res.string.list_section_jump_menu_title` → `"Jump to Category"`
- `Res.string.list_section_pester_badge` → `"Draft · Action Required"`
- `Res.string.list_jump_to_top_tooltip` → `"Jump to top"`
- `Res.string.list_jump_to_top_cd` → `"Scroll back to top of list"`

### 3. Sticky Headers on Compact Viewports vs Standard Layout on Wide Viewports
- **Compact Viewport (`LazyColumn`)**:
  - Utilizes `stickyHeader(key = "section_$sectionOrder", contentType = "header")`.
  - While stuck beneath `CenterAlignedTopAppBar`, renders with Material 3 surface elevation/tint and contains a quick jump action.
  - Tapping the jump action opens a menu listing the other available sections; selecting one triggers `listState.animateScrollToItem(targetIndex)`.
- **Medium & Expanded Viewports (`LazyVerticalGrid`)**:
  - Conforms to standard layout guidelines without forcing non-standard desktop sticky hacks: headers render as full-span row items (`span = { GridItemSpan(maxLineSpan) }`) introducing clear visual demarcation across columns.

### 4. Robust Scroll Position & "Jump to Top" Button
- **Explicit Hoisted States**:
  - `val listState = rememberLazyListState()` for `LazyColumn`.
  - `val gridState = rememberLazyGridState()` for `LazyVerticalGrid`.
- **Filter/Sort Intention Handler**:
  - When the user explicitly changes `statusFilter`, `ageFilter`, `sortColumn`, or `sortAscending`, smooth-reset or scroll cleanly to index 0 (`LaunchedEffect(content.statusFilter, content.ageFilter, content.sortColumn, content.sortAscending)`). This prevents the list from remaining awkwardly scrolled to an item index that changed or no longer exists.
- **Animated "Jump to Top" Action**:
  - Derived state: `val showJumpToTop by remember { derivedStateOf { (if (isCompact) listState.firstVisibleItemIndex else gridState.firstVisibleItemIndex) > 1 } }`.
  - When `showJumpToTop` is true, render a secondary/small FloatingActionButton (`Icons.Default.KeyboardArrowUp`) positioned above or alongside the main FAB with `AnimatedVisibility(fadeIn() + scaleIn(), fadeOut() + scaleOut())`.
  - Clicking it invokes `coroutineScope.launch { if (isCompact) listState.animateScrollToItem(0) else gridState.animateScrollToItem(0) }`.

### 5. FAB Clearance Bottom Padding Audit
Ensures floating action buttons never occlude content:
- **`JobApplicationsListScreen.kt`**:
  - Already defines `listPadding` with `bottom = MaterialTheme.spacing.fabBottomPadding` (`88.dp`). Maintain this on both `LazyColumn` and `LazyVerticalGrid`.
- **`JobApplicationDetailsScreen.kt`**:
  - **Issue Identified:** Lines 221 and 256 use `verticalScroll(rememberScrollState())` without bottom padding or spacers, causing the Events Timeline and Artifacts list at the bottom of the column to be hidden behind the BottomEnd FAB.
  - **Fix:** Add `Spacer(modifier = Modifier.height(MaterialTheme.spacing.fabBottomPadding))` at the bottom of both the compact single-column and wide two-column scrollable containers.

---

## User Review Required

> [!NOTE]
> - **Jump to Top Button Placement**:
>   - In `JobApplicationsListScreen.kt`, the primary FAB is in the bottom-right corner. The "Jump to Top" button will render directly above it as a compact `SmallFloatingActionButton` with an up arrow (`KeyboardArrowUp`), appearing only when scrolled past the first visible item.
> - **Bottom Clearance Padding**:
>   - `JobApplicationDetailsScreen.kt` will have `MaterialTheme.spacing.fabBottomPadding` added at the end of its scroll containers so the last timeline event is fully visible above the edit FAB.

---

## Proposed Changes

### `frontend/core/data`

#### [MODIFY] `TrackerStatus.kt`
- Add `sectionOrder: Int`, `cardSurfaceVariant: TopLevelCardSurfaceVariant`, `isSubduedCard: Boolean`, and `isActionRequiredCard: Boolean` properties to `TrackerStatus`.
- Implement these properties across `TrackerStatus.Known` entries and `TrackerStatus.Unrecognized`.

---

### `frontend/core/widgets`

#### [MODIFY] `JobApplicationCard.kt`
- Read `model.status.cardSurfaceVariant`, `model.status.isSubduedCard`, and `model.status.isActionRequiredCard`.
- Apply subdued alpha modifier (`Modifier.alpha(0.6f)`) when `isSubduedCard == true`.
- Render the pester/action badge in subtitle/summary when `isActionRequiredCard == true`.

---

### `frontend/features/job-applications-list/ui`

#### [MODIFY] `composeResources/values/strings.xml`
- Add string resources:
  - `list_section_interviewing`
  - `list_section_drafting`
  - `list_section_applied`
  - `list_section_rejected`
  - `list_section_jump_tooltip`
  - `list_section_jump_menu_title`
  - `list_section_pester_badge`
  - `list_jump_to_top_tooltip`
  - `list_jump_to_top_cd`

#### [NEW] `component/JobApplicationsSectionHeader.kt`
- Section header composable rendering:
  - Localized section title via `stringResource`.
  - Count badge (e.g. `(3)`).
  - Quick-jump button and dropdown menu allowing instant animated scroll to any other active section.

#### [MODIFY] `JobApplicationsListScreen.kt`
- Hoist `rememberLazyListState()` and `rememberLazyGridState()`.
- Add `showJumpToTop` derived state and render `SmallFloatingActionButton(onClick = { animateScrollToItem(0) })` stacked above the main FAB.
- Add `LaunchedEffect(content.statusFilter, content.ageFilter, content.sortColumn, content.sortAscending)` to reset scroll to top on filter/sort changes.
- Group `content.applications` by `item.status.sectionOrder`.
- In `LazyColumn` (Compact), wrap each section with `stickyHeader(key = "header_$order", contentType = "section_header")` passing `onJumpToSection`.
- In `LazyVerticalGrid` (Medium/Expanded), emit section headers using `item(key = "header_$order", span = { GridItemSpan(maxLineSpan) })`.

---

### `frontend/features/job-application-details/ui`

#### [MODIFY] `JobApplicationDetailsScreen.kt`
- Add `Spacer(modifier = Modifier.height(MaterialTheme.spacing.fabBottomPadding))` at the end of both the wide (two-column) and compact (single-column) scroll layouts to eliminate content truncation behind the FAB.

---

## Verification Plan

### Automated Tests
1. Run `./gradlew :frontend:features-job-applications-list-ui:jvmTest` to ensure viewmodel and list presentation tests continue passing.
2. Run `mise run dev:guards` to verify architectural rules and forbidden patterns.
3. Run `./gradlew :frontend:apps-android:assembleDebug` to compile Android debug binary.

### Manual Verification
1. Inspect the applications list:
   - Verify sections appear in exact order: 1. Interviewing, 2. In Draft, 3. Applied, 4. Rejected & Closed.
   - Verify card visual flair: Interviewing is bright and prominent, Draft has an action-required pester badge, Applied is standard, Rejected is subdued/disabled.
2. Scroll & Jump to Top:
   - Scroll down: verify "Jump to Top" button animates in above the FAB.
   - Tap "Jump to Top": verify list scrolls smoothly to index 0 and the button disappears.
   - Change filter or sort: verify the list resets to top cleanly without jumping or jittering.
   - Trigger pull-to-refresh: verify current scroll offset is preserved.
3. FAB Clearance Check:
   - In `JobApplicationDetailsScreen`, scroll all the way down to the timeline/events list and verify all content and the last event are 100% visible above the FAB.
