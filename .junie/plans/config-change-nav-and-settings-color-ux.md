---
sessionId: session-261111-184440-1ofu
---

# Technical Design

### Current Implementation

**Alpha flow (broken):**
1. `ColorPickerDialog` allows R/G/B/A input (4 channels)
2. `buildHexString()` produces `#AARRGGBB` when alpha < 255
3. `SettingsViewModel.handleChangeSeedColor()` only accepts 6-char hex — **rejects 8-char hex silently** (line 103: `normalized.length == 6`)
4. `App.kt` uses `toColorOrNull()` (RGB-only) to parse seed color for theme
5. Custom color row uses `toColorWithAlphaOrNull()` and checks for alpha < 255, but alpha can never be < 255

**Root cause:** The dialog supports alpha, but the ViewModel and theme application only support RGB. Alpha is silently discarded.

### Key Decisions
1. **Remove Alpha entirely** — rather than adding alpha support to the ViewModel and theme, remove it from the dialog. Material3 seed colors don't support alpha (they're used for color scheme derivation which requires opaque colors).
2. **Simplify custom color row** — remove checkerboard background and alpha-aware display logic since alpha is never stored.
3. **Keep RGBA utilities in `Color.kt`** — they're not harmful and may be useful for other features.

### Proposed Changes

**`SettingsScreen.kt`:**
- Remove `a`, `aText`, `aValid` state variables from `ColorPickerDialog`
- Remove Alpha `ChannelInputField` from the second row; Blue row becomes a single full-width field
- Remove alpha from `previewColor`: use `intArrayOf(previewR, previewG, previewB).toColor()` (size 3)
- Simplify `buildHexString()` to always produce `#RRGGBB`
- Remove `aText` from `LaunchedEffect` mode sync
- Update `allValid` to check only R, G, B
- Update dialog KDoc to remove alpha references
- In custom color row: replace `toColorWithAlphaOrNull()` with `toColorOrNull()`, remove `rgbaChannels`/`alpha`/`displayHex` logic, show `model.seedColorHex` directly
- Remove checkerboard gradient background — use simple swatch

**`SettingsUiIntent.kt`:**
- Update `SettingsUiModel` KDoc: change `"#RRGGBB" or "#AARRGGBB"` to `"#RRGGBB"`

### Architecture Diagram
``` mermaid
graph LR
  A[Custom color row] -->|tap| B[ColorPickerDialog RGB only]
  B -->|3 channels| C[R/G/B inputs]
  C -->|validates| D[Submit enabled]
  D -->|Submit| E[buildHexString #RRGGBB]
  E -->|dispatches| F[ChangeSeedColor intent]
  F -->|ViewModel| G[stores #RRGGBB]
  G -->|AppSettings| H[seedColorHex]
  H -->|App.kt| I[toColorOrNull RGB]
  I -->|Material3| J[Color scheme]
```

### Risks
- **No risks** — this change makes the UI match the actual backend behavior. Alpha was never functional.

# Delivery Steps

### ✓ Step 1: Remove alpha channel from ColorPickerDialog and custom color row
Remove the Alpha channel input from the dialog and simplify the custom color row to RGB-only.

**Changes in `SettingsScreen.kt`:**
- Remove `a`, `aText`, `aValid` state from `ColorPickerDialog`.
- Remove the Alpha `ChannelInputField` from the second row (Blue row becomes a single-field row).
- Remove alpha from `previewColor` calculation — use `intArrayOf(previewR, previewG, previewB).toColor()` (size 3).
- Simplify `buildHexString()` to always produce `#RRGGBB` (no alpha branching).
- Remove alpha from `LaunchedEffect` sync (`aText` removal).
- Update `allValid` to check only R, G, B.
- Update dialog KDoc to remove alpha references.
- In the custom color row: replace `toColorWithAlphaOrNull()` with `toColorOrNull()` for the swatch, remove the `rgbaChannels`/`alpha`/`displayHex` logic, and show `model.seedColorHex` directly.
- Remove the checkerboard gradient background (no transparency to show) — keep the simple swatch with optional border.

**Changes in `SettingsViewModel.kt`:**
- No changes needed (already strips alpha correctly).

**Changes in `SettingsUiIntent.kt`:**
- Update `SettingsUiModel` KDoc to remove `#AARRGGBB` reference — only `#RRGGBB`.