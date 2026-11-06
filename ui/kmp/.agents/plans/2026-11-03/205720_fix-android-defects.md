# Implementation Plan: Android Runtime, Build & Edge-to-Edge Defect Fixes

**Status:** Completed  
**Date:** 2026-11-03  
**Target:** ui/kmp:apps:android  

---

# Requirements

### Notes
* Use a Material3 Layout Scaffolding wrapper, like ListDetails

### Overview & Goals
The goal of this task is to resolve four specific Android UI defects in the JetBrains KMP Job Tracker application:
1. Ensure the app starts with the device's default dark/light theme setting.
2. Fix the FAB action menu alignment so it opens anchored to the bottom-right FAB rather than the bottom-left of the screen.
3. Ensure edge-to-edge support with proper text and icon contrast on status and navigation bars.
4. Mark required form fields with `(*)` and add the disclaimer text `"(*) Denotes a required field"` across form screens.

### Scope
#### In Scope
- Theme initialization logic in `App.kt` and `AppSettings`.
- Edge-to-edge window inset handling in `App.kt` and platform `SetupSystemBars` in `Theme.android.kt`.
- FAB menu positioning and anchoring in `App.kt` and `JobApplicationsListScreen.kt`.
- Form field label updates and legend addition in `AddJobApplicationScreen.kt` and `AddLeadScreen.kt`.

#### Out of Scope
- Major architectural changes to backend/repository layer.
- iOS or Desktop specific UI changes (unless affected by shared Composable updates).

### User Stories
- **As a user**, I want the app to respect my device's dark/light system theme setting when launched so that it matches my system appearance.
- **As a user**, I want the FAB menu to open directly above/next to the FAB in the bottom-right corner so that menu interaction is intuitive.
- **As a user**, I want status bar and navigation bar text/icons to remain readable regardless of the active theme and screen content.
- **As a user filling out a form**, I want clear visual indicators `(*)` for required fields and an explanatory legend so I know which fields are mandatory before saving.

### Functional Requirements
1. **Default Theme**: Upon initial startup, `AppSettings` must take `isSystemInDarkTheme()` into account so the application matches system dark/light mode by default.
2. **FAB Menu Alignment**: When tapping the FloatingActionButton in the bottom-right corner, the `DropdownMenu` must pop up anchored to the FAB on the bottom-right.
3. **Edge-to-Edge & System Bars**:
   - The app root background must draw edge-to-edge behind status and navigation bars.
   - Status bar and navigation bar icons must adjust contrast (`isAppearanceLightStatusBars` and `isAppearanceLightNavigationBars`) based on light/dark mode.
   - Screen contents must apply appropriate window insets without double padding.
4. **Required Form Fields**:
   - Add the notice text `"(*) Denotes a required field"` near the top of forms.
   - Append `(*)` to the label text for all required inputs (e.g. `"Company (*)"`).

# Technical Design

### Current Implementation
- `App.kt`: Instantiates `AppSettings()` which defaults `darkTheme = false` explicitly, overriding system dark mode preferences on initial launch. The root `Surface` applies `.safeContentPadding()`, which prevents the background color from drawing behind system bars. It also hosts `DropdownMenu` inside an unanchored full-screen `Box`, causing popup offset calculation to default toward bottom-left.
- `Theme.android.kt`: Configures `WindowCompat.getInsetsController()` to toggle `isAppearanceLightStatusBars` and `isAppearanceLightNavigationBars`.
- `JobApplicationsListScreen.kt`: Renders `Scaffold` with `FloatingActionButton` on bottom-right, but triggers external `fabMenuExpanded` state.
- `AddJobApplicationScreen.kt` & `AddLeadScreen.kt`: Use `supportingText = { Text("Required") }` for the `Company` field, but lack `(*)` in labels and do not display `"(*) Denotes a required field"`.

### Key Decisions
1. **System Theme Initialization**: Initialize `AppSettings` in `App.kt` using `isSystemInDarkTheme()` as default value for `darkTheme`, ensuring the "Dark Mode" checkbox in `SettingsDialog` accurately reflects the system default state on launch.
2. **Edge-to-Edge Insets**: Remove `.safeContentPadding()` from root `Surface` in `App.kt`. Allow `Scaffold` padding and `WindowInsets.safeDrawing` in individual screens to handle content insets while letting root background colors extend edge-to-edge.
3. **FAB Dropdown & Layout Scaffolding**: Ensure Material3 layout scaffolding (e.g. ListDetail/Scaffold structures) is used effectively, and pass `fabMenuExpanded` state or render `DropdownMenu` inside `floatingActionButton` layout scope in `JobApplicationsListScreen.kt` so Compose positions the popup relative to the FAB node.

### Proposed Changes

#### Common UI (`shared/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/`)
- **`App.kt`**:
  - Obtain `isSystemInDarkTheme()` and pass to initial `AppSettings` state so initial theme matches system settings.
  - Remove `.safeContentPadding()` from root `Surface`.
  - Pass FAB menu callbacks / state into `JobApplicationsListScreen` so the popup anchors to the FAB.
- **`ui/settings/SettingsDialog.kt`**:
  - Verify "Dark Mode" CheckboxRow displays `settings.darkTheme` state initialized from the system setting.
- **`ui/list/JobApplicationsListScreen.kt`**:
  - Anchor `DropdownMenu` directly inside `floatingActionButton` Box/Scope while retaining Material3 Scaffold structure.
- **`ui/add/AddJobApplicationScreen.kt`**:
  - Add `Text("(*) Denotes a required field", style = MaterialTheme.typography.bodySmall)` notice text at top of form.
  - Change `"Company"` label to `"Company (*)"`.
- **`ui/lead/AddLeadScreen.kt`**:
  - Add `Text("(*) Denotes a required field", style = MaterialTheme.typography.bodySmall)` notice text at top of form.
  - Change `"Company"` label to `"Company (*)"`.

#### Android Platform (`shared/src/androidMain/kotlin/dev/fnc314/tools/jobtracker/`)
- **`ui/component/Theme.android.kt`**:
  - Verify edge-to-edge system bar contrast handling with `WindowCompat`.

### Components
- `App`: Main application entry point managing theme state and top-level surface.
- `JobApplicationsListScreen`: List screen containing top app bar, search/filter chips, and bottom-right FAB with dropdown menu.
- `AddJobApplicationScreen` & `AddLeadScreen`: Form screens for creating new applications or leads with required input fields.

### File Structure
Modifications span existing files:
- `shared/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/App.kt`
- `shared/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/ui/list/JobApplicationsListScreen.kt`
- `shared/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/ui/add/AddJobApplicationScreen.kt`
- `shared/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/ui/lead/AddLeadScreen.kt`
- `shared/src/androidMain/kotlin/dev/fnc314/tools/jobtracker/ui/component/Theme.android.kt`

# Testing

### Validation Approach
Verification will involve checking UI layout, theme state, and edge-to-edge rendering behavior across affected components.

### Key Scenarios
1. **Device System Theme**:
   - Verify app initial launch matches dark mode when system is set to dark theme.
   - Verify app initial launch matches light mode when system is set to light theme.
2. **FAB Dropdown Menu**:
   - Tap FAB on bottom-right and verify `DropdownMenu` expands directly attached to the FAB on the bottom-right.
3. **Edge-to-Edge System Bars**:
   - Verify top status bar text/icons are legibly visible against background in both light and dark themes.
   - Verify bottom navigation bar buttons/icons are legibly visible in both light and dark themes.
4. **Form Required Fields**:
   - Open Add Application screen: verify `"(*) Denotes a required field"` text is visible and `Company (*)` field is clearly marked.
   - Open Add Lead screen: verify `"(*) Denotes a required field"` text is visible and `Company (*)` field is clearly marked.

# Delivery Steps

### ✓ Step 1: Fix default theme initialization and edge-to-edge system bar contrast
The app defaults to the device's system theme on initial launch and status/navigation bars render with proper contrast over edge-to-edge content.

- Initialize `AppSettings.darkTheme` in `App.kt` using `isSystemInDarkTheme()` so the app defaults to the system setting on launch.
  - The checkbox in the dialog must reflect this behavior
- Remove `safeContentPadding()` from the root `Surface` in `App.kt` to allow background colors to extend edge-to-edge behind system bars.
- Verify and refine `SetupSystemBars` in `Theme.android.kt` to correctly toggle light/dark status and navigation bar icon contrast based on the active theme.

### ✓ Step 2: Anchor FAB dropdown menu to bottom-right FAB
The FAB action menu is anchored directly to the bottom-right FloatingActionButton.

- Move `DropdownMenu` inside `JobApplicationsListScreen.kt` attached directly to the `FloatingActionButton` (or its container Box).
- Remove the detached root-level `Box` alignment logic for `fabMenuExpanded` in `App.kt`.
- Ensure menu items ("Add Application", "Add Lead", "Pending Edits") display anchored to the bottom-right FAB.

### ✓ Step 3: Add required field indicators and legend to form screens
Form screens display a clear required fields disclaimer and asterisk markers on required input fields.

- Add the `(*) Denotes a required field` notice text at the top of the form in `AddJobApplicationScreen.kt` and `AddLeadScreen.kt`.
- Update the label of required fields (e.g. `Company`) to include `(*)` in `AddJobApplicationScreen.kt` and `AddLeadScreen.kt`.
- Verify any dialogs or secondary form fields containing required inputs to ensure consistent asterisk labeling.