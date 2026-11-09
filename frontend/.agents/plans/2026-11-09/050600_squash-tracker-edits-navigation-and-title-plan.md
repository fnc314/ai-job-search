# Implementation Plan: Squash Duplicate Navigation Arrow and Title in Tracker Edits Screen

**Status:** Completed  
**Date:** 2026-11-09  
**Target:** `ui/kmp` (`features/tracker-edits/ui`, `apps/core`)  
**Depends On:** `core/navigation`  

---

## 1. Overview & Scope

### Goals
In the Tracker Edits screen (`/ui/kmp/features/tracker-edits/ui`), users see duplicate navigation chrome:
1. An outer back arrow and centered title `"Pending Edits"` rendered by the root `AppScaffold`'s `CenterAlignedTopAppBar`.
2. An inner back arrow and title `"Pending Edits"` rendered inside `TrackerEditsScreen.kt`'s custom toolbar row.
3. In addition, when on top-level navigation destinations (`ListRoute`, `EditsRoute`, `SettingsRoute`) accessible directly via the bottom `NavigationBar`, the top `AppScaffold` back arrow should not be shown, as those are primary root tabs.

### Scope
- **In Scope**:
  - Remove redundant inner back button and `"Pending Edits"` title in `TrackerEditsScreen.kt`.
  - Right-align action buttons (`Expire Stale Edits`, `Refresh`) in `TrackerEditsScreen.kt`.
  - Update `AppScaffold.kt` so `canNavigateBack` only displays the back arrow for pushed detail/sub-destinations (`DetailRoute`, `AddApplicationRoute`, `AddLeadRoute`), and not for top-level tabs (`ListRoute`, `EditsRoute`, `SettingsRoute`).
  - Update bottom bar tab navigation to prevent stacking top-level tabs.
  - Verify with unit tests, Konsist, Detekt, and Android assembleDebug.
- **Out of Scope**:
  - Modifying other feature flows or data layer contracts.

---

## 2. Proposed Changes

### `ui/kmp/features/tracker-edits/ui`
- In `TrackerEditsScreen.kt`:
  - Delete inner Back `IconButton` and title `Text`.
  - Align action buttons (`ExpireStaleEdits`, `Refresh`) to `Arrangement.End` across the row.

### `ui/kmp/apps/core`
- In `AppScaffold.kt`:
  - Set `isTopLevelDestination = isListSelected || isEditsSelected || isSettingsSelected`.
  - Set `canNavigateBack = navigator.backStack.size > 1 && !isTopLevelDestination`.
  - On bottom navigation item click, navigate cleanly without accumulating root tab stacks.

---

## 3. Verification Plan
- Unit tests: `:features-tracker-edits-ui:jvmTest`, `:features-tracker-edits-data:jvmTest`.
- Konsist architecture tests: `mise run dev:konsist`.
- Detekt: `mise run ui:kmp:detekt`.
- Android build: `./gradlew :apps-android:assembleDebug`.
