---
sessionId: session-261110-224350-s0bw
---

# Requirements

## Overview & Goals

Fix the KMP Inspector integration so that Ktor network traffic is captured and inspectable on Android (and iOS/JVM). The inspector must respect the Settings toggle (`enableKmpInspector`) reactively — toggling it on/off should immediately enable/disable the inspector. Additionally, restructure the navigation UI so Settings is accessible from a gear icon in the top-left of the top app bar, and remove Settings from the navigation rail and bottom bar.

## Current State (After RM-007 Initial Fix)

The previous commit (`f1cde525`) wired the basic KMP Inspector infrastructure:
- `KmpInspector.install(this)` is called in `JobTrackerApplication.onCreate()`
- `setInspectorEnabled()` expect/actual exists and is called in `App.kt`
- iOS/JVM use the actual `KmpInspector` composable wrapper
- `attachInspector()` placeholder functions exist (no-ops due to Room 3 incompatibility)
- `installInspector()` IS called in `createJsonHttpClient()`

**But the inspector still doesn't work on Android because:**

### Root Cause 1: Inspector Enabled Before Settings Load
`KmpInspector.install(this)` runs in `Application.onCreate()` before the DataStore-based Settings are loaded. The inspector defaults to `enabled=true` (debuggable build). The `setInspectorEnabled(settings.enableKmpInspector)` call in `App.kt` does run reactively when settings change, but the Settings UI only saves to DataStore — it does NOT directly call `setInspectorEnabled()`. So the toggle changes the stored value, but `App.kt` recomposes with the new settings and calls `setInspectorEnabled()` — this SHOULD work. **However**, the real problem is that `Inspector.enabled` must be set BEFORE the Ktor HttpClient captures requests. Since the HttpClient is a Koin singleton created before the Settings toggle is flipped, the inspector plugin may have already captured requests before `Inspector.enabled` was set.

### Root Cause 2: Settings Toggle Doesn't Call setInspectorEnabled Directly
The Settings ViewModel (`SettingsViewModel.kt`) handles `ToggleKmpInspector` by calling `updateSettingsUseCase()`, which saves to DataStore. The `App.kt` composable collects `settings` and calls `setInspectorEnabled(settings.enableKmpInspector)`. This SHOULD work reactively. But the issue is that `setInspectorEnabled` is called in the composable body of `App.kt` which is inside a `JobTrackerTheme` block — if the Settings screen is in a different part of the tree, the `App.kt` composable may not recompose when settings change (since `App.kt` collects settings via `koinInject<AppSettingsRepository>()` and `collectAsState()` — this SHOULD trigger recomposition).

**The actual bug**: `setInspectorEnabled` is called correctly and reactively. But on Android, the inspector's `Inspector.enabled` switch only controls whether the bubble appears and whether NEW requests are captured. Requests that were already captured before the toggle remain visible. And if the inspector was installed with `enabled=true` (default), it captures everything from startup.

### Root Cause 3: No Settings Shortcut in Top Bar
Settings is only accessible via the navigation rail (desktop) or bottom bar (mobile). The user wants a Settings gear icon in the top-left of the top app bar for quick access.

## Scope

### In Scope
- Fix KMP Inspector to respect the Settings toggle reactively on Android
- Ensure network traffic is captured when enabled
- Add Settings gear icon to the top-left of the top app bar (navigationIcon area)
- Remove Settings from the navigation rail and bottom navigation bar
- Update ROADMAP.md (RM-007 → DONE)

### Out of Scope
- Room database inspection (kmp-inspector-room doesn't support Room 3)
- Adding kmp-inspector support to JS/WASM (not supported by the library)
- Adding new panels or features to the inspector

# Technical Design

## Current Implementation

### Key Files
- `frontend/apps/core/src/commonMain/.../KmpInspector.kt` — expect declaration for `KmpInspector` composable and `setInspectorEnabled()`
- `frontend/apps/core/src/androidMain/.../KmpInspector.android.kt` — no-op composable, `setInspectorEnabled` sets `Inspector.enabled`
- `frontend/apps/core/src/iosMain/.../KmpInspector.ios.kt` — actual `LibKmpInspector` wrapper
- `frontend/apps/core/src/jvmMain/.../KmpInspector.jvm.kt` — actual `LibKmpInspector` wrapper
- `frontend/apps/android/.../JobTrackerApplication.kt` — calls `KmpInspector.install(this)` in `onCreate()`
- `frontend/apps/core/src/commonMain/.../App.kt` — calls `setInspectorEnabled(settings.enableKmpInspector)` and `KmpInspector(enabled, content)`
- `frontend/core/network/.../NetworkClient.kt` — calls `installInspector()` in `createJsonHttpClient()`
- `frontend/core/network/.../InspectorExt.android.kt` — installs `KmpInspectorPlugin` (Ktor)
- `frontend/features/settings/ui/.../SettingsViewModel.kt` — handles `ToggleKmpInspector` via `updateSettingsUseCase`
- `frontend/apps/core/src/commonMain/.../ui/AppScaffold.kt` — scaffold with nav rail, bottom bar, and top app bar

### kmp-inspector API (from docs)
- **Android**: `KmpInspector.install(this)` in Application draws the bubble. `Inspector.enabled` controls whether it runs. Do NOT wrap Compose content or you get two bubbles.
- **iOS/JVM**: Use `KmpInspector { }` composable wrapper.
- **Ktor**: Install `KmpInspectorPlugin` (done via `installInspector()`).
- **JS/WASM**: Not supported.

## Key Decisions

1. **Android inspector enable/disable**: Set `Inspector.enabled = false` as the default in `JobTrackerApplication.onCreate()` before `KmpInspector.install(this)`. Then `setInspectorEnabled()` in `App.kt` will correctly enable it when the user toggles it on. This ensures the inspector starts disabled and only activates when the user explicitly enables it.
2. **Settings toggle reactivity**: The current flow (Settings UI → DataStore → App.kt recomposes → `setInspectorEnabled()`) is correct. No changes needed to the toggle mechanism — just fix the initial state.
3. **Settings in top bar**: Add a Settings gear icon as the `navigationIcon` in `CenterAlignedTopAppBar` when on a top-level destination (replacing the back arrow logic). When on a child destination, the back arrow remains. This avoids adding a second action cluttering the right side.
4. **Remove Settings from nav rail/bar**: Remove `SettingsRoute` from the `topLevelDestinations` list in `AppScaffold.kt`. Update `isTopLevelDestination` to not include `isSettingsSelected`. Settings becomes a modal-like destination accessible from the top bar.

## Proposed Changes

### Stage 1: Fix Android Inspector Initial State

**`frontend/apps/android/.../JobTrackerApplication.kt`**
- Set `Inspector.enabled = false` BEFORE `KmpInspector.install(this)` so the inspector starts disabled
- This ensures the inspector only activates when the user toggles it via Settings

### Stage 2: Add Settings Shortcut to Top Bar and Remove from Nav Rail

**`frontend/apps/core/src/commonMain/.../ui/AppScaffold.kt`**
- Remove `SettingsRoute` from `topLevelDestinations` list (lines 142-148)
- Update `isTopLevelDestination` to exclude `isSettingsSelected` (line 103)
- Change `navigationIcon` in `CenterAlignedTopAppBar`:
  - When `canNavigateBack`: show back arrow (existing behavior)
  - When on top-level destination and authenticated: show Settings gear icon that navigates to `SettingsRoute`
- Update `selected` logic in NavigationBar and NavigationRail to no longer include Settings

### Stage 3: Verify and Document

- Compile all targets: JVM, JS, WASM, Android
- Verify inspector starts disabled on Android
- Verify Settings toggle enables/disables inspector reactively
- Verify Settings is accessible from top bar gear icon
- Verify Settings no longer appears in nav rail or bottom bar
- Update ROADMAP.md (RM-007 → DONE)

# Delivery Steps

### ✓ Step 1: Fix Android Inspector initial state and verify Settings toggle reactivity
The inspector starts enabled on Android because `KmpInspector.install(this)` defaults to `enabled=true` (debuggable build). Set it disabled initially so the Settings toggle has control.

- In `JobTrackerApplication.kt`, add `Inspector.enabled = false` before `KmpInspector.install(this)` so the inspector starts disabled.
- Verify that toggling `enableKmpInspector` in Settings enables the inspector (bubble appears, network traffic captured).
- Verify that toggling it off disables the inspector (bubble hidden, no new captures).
- Run `compileDebugKotlin` (apps-android) to verify compilation.

### ✓ Step 2: Add Settings gear icon to top bar and remove from navigation rail
Settings should be accessible from a gear icon in the top-left of the top app bar, not from the navigation rail or bottom bar.

- In `AppScaffold.kt`, remove `SettingsRoute` from the `topLevelDestinations` list.
- Update `isTopLevelDestination` to exclude `isSettingsSelected` so Settings is treated as a child destination.
- Change `navigationIcon` in `CenterAlignedTopAppBar`: show a Settings gear icon when on a top-level destination (authenticated, no back stack), and keep the back arrow when on a child destination.
- The gear icon should navigate to `SettingsRoute` via `navigator.navigate(SettingsRoute)`.
- Remove Settings from the `selected` logic in both NavigationBar and NavigationRail.
- Run `compileKotlinJvm`, `compileKotlinJs`, `compileKotlinWasmJs`, and `compileDebugKotlin` to verify compilation across all targets.

### ✓ Step 3: Update ROADMAP and save plan document
Mark RM-007 as DONE and document the changes.

- Update ROADMAP.md to mark RM-007 as DONE.
- Update `frontend/.agents/plans/2026-11-10/231600_fix-kmp-inspector-rm007.md` with the final implementation details.
- Update `frontend/.agents/plans/README.md` if needed.