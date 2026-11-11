---
sessionId: session-261111-011217-6pkn
---

# Requirements

### Overview
Fix two issues with the KMP Inspector and top app bar layout:
1. **KMP Inspector not working on Android** — investigate and fix the root cause.
2. **Settings gear icon** — move it from the top-left (`navigationIcon`) to the top-right (`actions`) of the top app bar as the **only** icon in that position.
3. **All other action icons** — remove the global refresh icon and the globe (website) icon from the top app bar.

### Scope
- **In Scope:** Fix KMP Inspector on Android, reposition settings gear icon to top-right as the sole action icon, remove all other icons (refresh, globe) from `AppScaffold`.
- **Out of Scope:** Room inspector (deferred — Room 3 incompatibility), JS/WASM inspector support (library limitation).

### Functional Requirements
- KMP Inspector floating bubble appears and captures Ktor network traffic on Android when `enableKmpInspector` is toggled in Settings.
- **Only** the settings gear icon appears in the top-right corner of the top app bar when on a top-level destination. No other icon should ever appear there.
- Refresh icon is no longer shown in the global top app bar; per-screen refresh controls (e.g., in `JobApplicationsListScreen`) remain functional.
- Globe (website) icon is no longer shown in the global top app bar.

# Technical Design

### Current Implementation

**KMP Inspector:**
- `JobTrackerApplication.kt` calls `KmpInspector.install(this)` and sets `Inspector.enabled = false` before install.
- `apps-core/build.gradle.kts` declares `libs.kmp.inspector` for `androidMain`, `iosMain`, and `jvmMain`.
- `core/network/build.gradle.kts` declares `libs.kmp.inspector` and `libs.kmp.inspector.ktor` for `androidMain`.
- `InspectorExt.android.kt` installs `KmpInspectorPlugin` (Ktor plugin) on Android.
- `KmpInspector.android.kt` is a no-op composable (just renders `content()`), relying on the floating bubble from `KmpInspector.install()`.
- `createJsonHttpClient()` in `NetworkClient.kt` calls `installInspector()` to install the Ktor plugin.
- The inspector is toggled via `setInspectorEnabled()` which sets `Inspector.enabled`.

**Top App Bar (AppScaffold.kt):**
- Settings gear icon is in `navigationIcon` slot (top-left), shown when on a top-level destination.
- Globe icon is in `actions` slot (top-right), shown only on `CompanyDetailsRoute` when a website URL is present.
- Refresh icon is in `actions` slot (top-right), shown globally for all authenticated users.
- `onRefreshClick` callback is passed from `App.kt` but wired to a no-op `{}` by default.

### Key Decisions
- The KMP Inspector on Android relies on the floating bubble from `KmpInspector.install()`. The no-op composable is correct — Android doesn't need a Compose wrapper. The issue is likely that the inspector starts disabled and the toggle doesn't re-enable the bubble properly, or the Ktor plugin isn't capturing traffic because the HttpClient is created before the inspector is enabled.
- Move settings gear to `actions` slot (top-right) for a more conventional placement.
- The settings gear must be the **only** icon in the `actions` slot — no other icon should ever appear there.
- Remove global refresh icon since `JobApplicationsListScreen` already has a local refresh `TooltipIconButton`.
- Remove globe (website) icon from `CompanyDetailsRoute` as it is not a core navigation action.

### Proposed Changes

**1. Fix KMP Inspector on Android:**
- The inspector is installed in `JobTrackerApplication.onCreate()` before the app is fully initialized. The Ktor plugin is installed via `installInspector()` in `createJsonHttpClient()`. The issue is that `Inspector.enabled = false` is set before `KmpInspector.install()`, and when the user toggles the setting, `setInspectorEnabled(true)` sets `Inspector.enabled = true`, but the floating bubble may not appear because the inspector was installed in a disabled state. The fix is to ensure the inspector is properly re-enabled when the toggle changes.

**2. Move Settings Gear to Top-Right (Sole Action Icon):**
- In `AppScaffold.kt`, move the settings `IconButton` from `navigationIcon` to the `actions` block.
- The `navigationIcon` slot will only show the back button when `canNavigateBack`.

**3. Remove All Other Action Icons:**
- Remove the refresh `IconButton` from the `actions` block in `AppScaffold.kt`.
- Remove the globe (website) `IconButton` from the `actions` block in `AppScaffold.kt`.
- Remove the `onRefreshClick` parameter from `AppScaffold` and its callers.
- Remove unused imports: `Icons.Filled.Refresh`, `Icons.Filled.Public`, `PlatformFileOpener`, and related string resources (`app_action_refresh`, `app_action_globe`).

### File Structure
- **Modified:** `frontend/apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/ui/AppScaffold.kt`
- **Modified:** `frontend/apps/core/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/app/core/App.kt` (remove `onRefreshClick` wiring if present)
- **Modified:** `frontend/apps/android/src/main/kotlin/dev/fnc314/tools/jobtracker/app/android/JobTrackerApplication.kt` (fix inspector initialization)

# Delivery Steps

### ✓ Step 1: Move settings gear to top-right and remove all other action icons
### ✓ Step 2: Fix KMP Inspector on Android
Move the settings gear icon from `navigationIcon` (top-left) to the `actions` slot (top-right) of `CenterAlignedTopAppBar` in `AppScaffold.kt`, making it the only icon in that position. Remove the globe and refresh icons entirely.

- In `AppScaffold.kt`, move the settings `IconButton` (lines 163-169) from `navigationIcon` to the `actions` block.
- Remove the globe `IconButton` (lines 173-180) from the `actions` block entirely.
- Remove the refresh `IconButton` (lines 182-189) from the `actions` block entirely.
- Remove unused imports: `Icons.Filled.Refresh`, `Icons.Filled.Public`, `PlatformFileOpener`, `Res.string.app_action_refresh`, `Res.string.app_action_globe`.
- Remove the `onRefreshClick` parameter from `AppScaffold` and its callers in `App.kt`.
- The `navigationIcon` slot will now only show the back button when `canNavigateBack`.
- The refresh icon already exists locally in `JobApplicationsListScreen.kt` as a `TooltipIconButton`, so per-screen refresh is preserved.