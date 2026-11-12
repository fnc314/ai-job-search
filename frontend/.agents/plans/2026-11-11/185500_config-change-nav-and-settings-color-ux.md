# Implementation Plan: Config-Change Navigation and Settings Color Picker Dialog

**Status:** Completed  
**Date:** 2026-11-11  
**Target:** `frontend`  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261111-184440-1ofu  

---

## 1. Executive Summary & Goals

Three issues affect the frontend's user experience on foldable devices and in the Settings screen:

1. **Configuration-change navigation loss**: On a Galaxy Fold6 (or any device), opening the fold or rotating the screen resets the app to the initial list screen, losing the user's navigation position (e.g., details screen, scroll position, etc.).
2. **Settings color input UX**: The custom theme color hex input in Settings was difficult to use — intermediate typing was silently ignored, there was no visual feedback for invalid input, and the `#` prefix handling was inconsistent.
3. **Color picker enhancement**: The hex-only input was replaced with a full color picker dialog inspired by Android's TimePickerDialog, providing RGBA channel control, a large preview region, and decimal/hex input modes.

**Goals:**
- Ensure device rotation and shape-change preserve all temporary application state (navigation, scroll, etc.).
- Replace the hex text field with a full color picker dialog for precise, accessible color control.
- Extend color utilities to support RGBA parsing and formatting with alpha channel.

## 2. Architecture & Design Principles

### Navigation State Preservation
- Opening/closing a foldable, rotating the device, or any configuration change must preserve:
  - Current navigation destination (e.g., JobApplication details, Company details)
  - Full back stack history
  - Scroll positions within screens (handled by Navigation3 entry retention)
- The app must not reset to the Job Applications List after a config change

### Color Picker Dialog
- Triggered by tapping the current custom seed color row (showing a color swatch and hex value)
- 4 input fields: Red, Green, Blue, Alpha — each accepting 0-255 (decimal) or 00-FF (hex)
- A toggle switch to switch between decimal and hex input mode (affects all 4 fields)
- A large color preview region (≥50% of dialog area, 200dp height) showing the current parsed color with alpha
- A "Submit" button — enabled only when all 4 channels have valid values
- A "Cancel" button — always enabled; dismisses the dialog without changing the color
- The dialog initializes with the current seed color values
- On Submit, the color is applied via the existing `ChangeSeedColor` intent

### Scope
**In Scope:**
- `App.kt` — auth initialization and navigation state lifecycle
- `AppNavigatorImpl.kt` — `login()` idempotency
- `SettingsScreen.kt` — replaced hex field with dialog trigger, added `ColorPickerDialog` composable
- `Color.kt` — extended RGBA utilities for parsing and formatting

**Out of Scope:**
- `SettingsViewModel.kt` — no changes needed; the dialog dispatches the existing `ChangeSeedColor` intent
- `SettingsUiIntent.kt` / `SettingsUiState.kt` — no changes needed
- Multi-pane / list-detail adaptive layouts (separate concern)
- Process-death state restoration beyond what Navigation3's `rememberNavBackStack` provides
- Adding a color palette/picker wheel
- HSL/HSV color space support

## 3. Proposed Changes & Implementation Steps

### Issue 1: Configuration-Change Navigation Loss

**Root Cause:** `LaunchedEffect(isAuthenticated)` fires `appNavigator.login()` → `switchRoot(JobApplicationListRoute)` after every config change, clearing the back stack that `rememberNavBackStack` just restored. The `authInitialized` state is also not saved, causing unnecessary early returns.

**Changes:**
- **`App.kt`**: Replace `mutableStateOf(false)` for `authInitialized` with `rememberSaveable { mutableStateOf(false) }` so auth initialization is not re-run on config changes. Guard the `LaunchedEffect(isAuthenticated)` so `login()` is only called when `authInitialized` transitions from `false` to `true`, not on every recomposition after a config change. Add a `rememberSaveable` flag tracking whether `login()` has already been called for the current auth session, preventing `switchRoot` from firing on restored compositions.
- **`AppNavigatorImpl.kt`**: Update `login()` to check if the current back stack root is already `JobApplicationListRoute` before calling `switchRoot`, avoiding unnecessary back stack resets even if `login()` is called redundantly.

### Issue 2: Settings Color Picker Dialog

**Current Problems:**
- Hex-only input is not accessible or precise for color selection
- No alpha channel support
- No visual preview of the color while adjusting

**Changes:**
- **`Color.kt`**: Added RGBA utilities — `toRgbChannels()`, `toRgbaChannels()`, `IntArray.toColor()` factory, `toColorWithAlphaOrNull()` for parsing `#RRGGBB`/`#AARRGGBB`, and updated `toHexString(includeAlpha)` with a default parameter for backward compatibility.
- **`SettingsScreen.kt`**: Replaced the hex `OutlinedTextField` with a clickable `Row` showing a color swatch and hex value. Added `ColorPickerDialog` composable with:
  - `AlertDialog` wrapper with `@OptIn(ExperimentalMaterial3Api::class)`
  - Large color preview `Box` (200dp height, ≥50% of dialog) with dynamic background color
  - Decimal/Hex mode toggle using `Switch`
  - 4 `OutlinedTextField` inputs in a 2x2 grid for R, G, B, A channels
  - Each field validates independently (0-255 decimal or 00-FF hex)
  - Submit button enabled only when all 4 channels are valid
  - Cancel button always enabled
  - Initialize R/G/B/A from `currentHex.toColorWithAlphaOrNull()`
  - On Submit: construct hex string (`#RRGGBB` if alpha=255 for backward compat, `#AARRGGBB` otherwise), dispatch `ChangeSeedColor`
  - On Cancel/Dismiss: no state change
  - `ChannelInputField` helper composable for individual channel inputs

### Architecture Diagram
```mermaid
graph LR
  A[SettingsScreen color row] -->|tap| B[showColorPickerDialog = true]
  B -->|renders| C[ColorPickerDialog]
  C -->|init from| D[model.seedColorHex]
  C -->|local state| E[R/G/B/A values]
  C -->|local state| F[isHexMode toggle]
  E -->|validates| G[Submit enabled?]
  C -->|preview| H[Color preview Box]
  G -->|Submit| I[onConfirm hex string]
  I -->|dispatches| J[ChangeSeedColor intent]
  C -->|Cancel| K[onDismiss]
```

## 4. Verification Plan & Testing Checklist

### Navigation State Preservation
- Navigate to a Job Application details screen → trigger config change (rotation or foldable open) → verify the details screen is still displayed with correct data
- Navigate to a Company details screen → trigger config change → verify the details screen is preserved
- Navigate through multiple screens (List → Details → Settings) → trigger config change → verify back stack is intact and back navigation works
- Trigger config change while on the List screen → verify no regression (stays on List)

### Color Picker Dialog
- Tap the custom seed color row → dialog opens with current color values
- Dialog initializes R/G/B/A from the current `seedColorHex`
- Enter 0-255 in each field in decimal mode → preview updates, submit enabled when all valid
- Enter 256 → field shows error, submit disabled
- Toggle to hex mode → fields show 00-FF values
- Enter "FF" in Red → preview updates to red
- Enter "GG" → field shows error, submit disabled
- Preview region updates in real-time as any channel changes
- Alpha channel affects preview opacity
- Submit with valid values → color applied, dialog dismisses
- Submit with alpha=255 → stored as `#RRGGBB` (backward compat)
- Submit with alpha<255 → stored as `#AARRGGBB`
- Cancel → dialog dismisses, color unchanged
- Back press → dialog dismisses, color unchanged

### Edge Cases
- Config change during auth initialization (rare race condition) → app should not crash
- Current color is invalid hex → dialog initializes with defaults (Professional Blue)
- Config change while dialog is open → dialog state is lost (acceptable — ephemeral UI)
- Empty input in any field → submit disabled, inline error shown
- Process death (beyond config change) → Navigation3 `rememberNavBackStack` handles this via `SavedStateRegistry`; back stack should restore

### Build Verification
- `:frontend:apps-android:assembleDebug` builds successfully with no compilation errors

## 5. Bookkeeping
- [x] Plan persisted at `frontend/.agents/plans/2026-11-11/185500_config-change-nav-and-settings-color-ux.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
