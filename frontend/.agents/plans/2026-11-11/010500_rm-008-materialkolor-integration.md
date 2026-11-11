# Implementation Plan: RM-008 MaterialKolor Integration

**Status:** Completed  
**Date:** 2026-11-11  
**Target:** `frontend`  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** junie-session-261110-224350-s0bw  

---

## 1. Executive Summary & Goals

Integrate MaterialKolor (v6.0.0) into the `frontend/core/design-system` module to replace manual color scheme derivation with Google's official `material-color-utilities` engine. This provides scientifically accurate Material 3 color harmonization across all platforms (Android, iOS, Desktop JVM, JS, Wasm).

**In Scope:**
- Add MaterialKolor dependency (`com.materialkolor:material-kolor-material3:6.0.0`) to the version catalog and design-system module
- Replace manual color scheme derivation in `Theme.kt` with MaterialKolor's `DynamicMaterialExpressiveTheme`
- Mirror the dependency to the tools version catalog
- Update `ROADMAP.md` RM-008 status to `DONE`

**Out of Scope:**
- Changing seed color options or the `SeedColors` object
- Modifying the Settings UI for theme customization
- Platform-specific theme files (they only handle system bars via `SetupSystemBars`)
- Migration to alternative palette styles (using default `TonalSpot`)

**Functional Requirements:**
- The theme continues to accept a `seedColor` parameter (defaulting to `#2F5597` Professional Blue)
- Dark/light mode switching continues to work via `darkTheme` parameter
- All existing color roles (primary, secondary, tertiary, surface, etc.) remain available through `MaterialTheme.colorScheme`
- The `SeedColors` object and hex parsing utilities (`toColorOrNull`, `toHexString`) remain unchanged
- No visual regression — colors should be more accurate per Material Design spec

## 2. Architecture & Design Principles

### Current Implementation (Before Changes)

The design system (`frontend/core/design-system`) previously used:
- **`Color.kt`**: Contained only `SeedColors` object with predefined seed colors, and hex parsing utilities (`toColorOrNull`, `toHexString`). No manual color scheme builder functions existed — color derivation was handled within `Theme.kt`.
- **`Theme.kt`**: `JobTrackerTheme()` composable that derived Material 3 colors from a seed color, then wrapped content in `MaterialExpressiveTheme` (from Compose Material 3 alpha).
- **Version catalog**: `gradle/libs.versions.toml` manages all dependencies via type-safe catalogs.
- **Design-system build.gradle.kts**: Depends on `libs.bundles.compose.ui` which includes `compose-material3`.

### Key Decisions

**Use `material-kolor-material3` adapter** — The project uses Material 3 Expressive (`MaterialExpressiveTheme`), so the `material-kolor-material3` artifact is the correct choice. It provides `DynamicMaterialExpressiveTheme` which wraps `MaterialExpressiveTheme` and generates color schemes from a seed using Google's `material-color-utilities` engine.

**Keep `SeedColors` object and hex utilities** — These are used by the Settings UI for theme customization and are unaffected by MaterialKolor.

**Default to `TonalSpot` palette style** — MaterialKolor defaults to `TonalSpot`, which produces colors closest to standard Material 3 derivation.

### Architecture Diagram

```mermaid
graph LR
    SK[SeedColors object] --> TH[Theme.kt]
    MK[MaterialKolor lib] --> TH
    TH --> DMET[DynamicMaterialExpressiveTheme]
    DMET --> MS[material-color-utilities engine]
    DMET --> MT[MaterialExpressiveTheme]
    MT --> UI[App Composables]
    SK -.seedColor.-> DMET
```

### Risks

- **MaterialKolor 6.0.0 stability**: The library was recently released (~27 days ago per Maven Central). Mitigation: pinned to exact version `6.0.0` in the catalog.
- **Color appearance change**: MaterialKolor uses Google's official algorithm, so colors differ slightly from any previous manual derivation. This is an improvement, not a regression.
- **Compose Multiplatform compatibility**: MaterialKolor supports Android, iOS, JVM, JS, and Wasm — matching the project's targets exactly.

## 3. Proposed Changes & Implementation Steps

### Step 1: Add MaterialKolor dependency to version catalog and design-system module

**Files Modified:**
| File | Action |
|------|--------|
| `gradle/libs.versions.toml` | Modified — add version + library |
| `gradle/tools/version-catalogs/src/main/resources/libs.versions.toml` | Modified — mirror version + library |
| `frontend/core/design-system/build.gradle.kts` | Modified — add dependency |

- Added `materialKolor = "6.0.0"` version entry to `gradle/libs.versions.toml` `[versions]` section
- Added `materialKolor-material3 = { module = "com.materialkolor:material-kolor-material3", version.ref = "materialKolor" }` to `[libraries]` section
- Mirrored both entries to `gradle/tools/version-catalogs/src/main/resources/libs.versions.toml`
- Added `api(libs.materialKolor.material3)` dependency to `frontend/core/design-system/build.gradle.kts` `commonMain.dependencies`
- Verified the project syncs and resolves the new dependency without errors

### Step 2: Replace color derivation with MaterialKolor in Theme module

**Files Modified:**
| File | Action |
|------|--------|
| `frontend/core/design-system/src/commonMain/.../Theme.kt` | Modified — use DynamicMaterialExpressiveTheme |
| `ROADMAP.md` | Modified — update RM-008 status |

- Replaced the color scheme derivation + `MaterialExpressiveTheme` call in `Theme.kt` with `DynamicMaterialExpressiveTheme(seedColor, isDark, typography, shapes, content)`
- Updated imports in `Theme.kt` to use MaterialKolor's `DynamicMaterialExpressiveTheme`
- Kept `SetupSystemBars`, `CompositionLocalProvider`, typography, and shapes configuration in `Theme.kt`
- `Color.kt` required no changes — it already contained only `SeedColors`, `toColorOrNull`, and `toHexString`
- Verified the project compiles across all targets (desktop, Android, JS/Wasm)
- Updated RM-008 status in `ROADMAP.md` to `DONE` with completion date

## 4. Verification Plan & Testing Checklist

- [x] Project syncs and resolves MaterialKolor 6.0.0 dependency without errors
- [x] `frontend/core/design-system` compiles across all targets (desktop, Android, JS/Wasm)
- [x] `JobTrackerTheme(seedColor, darkTheme)` signature remains unchanged
- [x] Dark/light mode switching continues to work via `isDark` parameter
- [x] All color roles (primary, secondary, tertiary, surface, etc.) accessible via `MaterialTheme.colorScheme`
- [x] `SeedColors.All` preset swatches still available for Settings UI
- [x] `toHexString()` and `toColorOrNull()` utilities still work
- [x] Tools version catalog mirrored with MaterialKolor entries
- [x] Settings feature integrations unchanged (no modifications needed in `features/settings`)
- [x] ROADMAP.md updated (RM-008 → DONE)

## 5. Bookkeeping
- [x] Plan persisted at `frontend/.agents/plans/2026-11-11/010500_rm-008-materialkolor-integration.md`
- [x] Registry `frontend/.agents/plans/README.md` row added / status synchronized
- [x] ROADMAP.md linked plans updated to point to the correct plan location
