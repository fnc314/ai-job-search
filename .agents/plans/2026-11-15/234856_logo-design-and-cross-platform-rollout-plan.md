# Implementation Plan: AI Job Tracker Logo & Cross-Platform Asset Rollout

**Status:** Completed  
**Date:** 2026-11-15  
**Target:** Monorepo brand identity, Android Adaptive Icon, iOS AppIcon, Compose vector/animation, Web favicon, Desktop icons  
**Depends On:** `frontend/core/design-system`, `frontend/apps/ios`, `frontend/apps/android`, `frontend/apps/web`  

---

## Goal Description
Design and roll out a clean, professional, abstract/geometric brand logo for **AI Job Tracker**.

Per user direction:
- **No Character/Mascot:** Eliminate any courier bird or mascot character.
- **Direction:** Modern geometric **Paper Glider (Arrow/Delta) + Orbital Target Arc + 4-Point AI Spark**.
- **Cross-Platform Parity:**
  - **Android Adaptive Icon:** Layered foreground (`108dp x 108dp`, strict `72dp` safe circle) + background (`#2F5597`) + Android 13+ themed monochrome vector.
  - **iOS App Icon:** Apple Asset Catalog (`Assets.xcassets/AppIcon.appiconset`) with modern 1024x1024 universal canvas plus iOS 18 Light, Dark, and Tinted appearance variants.
  - **Jetpack Compose:** Animatable multi-layer Vector / Canvas Composable (`AnimatedBrandLogo`) in `core-design-system`.
  - **Web Favicon & Loader:** Multi-resolution `favicon.ico` (16, 32, 48px), modern dark/light `favicon.svg`, and animated loading state in `index.html`.
  - **Desktop:** macOS `.icns`, Windows `.ico`, and Linux PNG for CMP desktop distribution.

---

## Visual Design Specification: "The Delta Glider & Compass"

```
            ✦ (AI North Star Spark #F59E0B)
           / \
          /   \
         /  ▲  \  (Supersonic Geometric Paper Glider #FFFFFF / #E2E8F0)
        /  / \  \
       /  /   \  \
      /  /  |  \  \
     /  /   |   \  \
    /  /____|____\  \
   (                 )  (Orbital Target Arc #38BDF8 / stroke-opacity 0.5)
    \_______________/
```

### 1. Layers & Color Palette
- **Background Layer (`bg`):**
  - Canonical `SeedColors.ProfessionalBlue` (`#2F5597`) with subtle linear/radial gradient to Deep Navy (`#1B365D`).
- **Midground Layer (`glider`):**
  - Minimalist 3-facet origami paper airplane / delta wing ascending upwards at a dynamic 45° angle.
  - Top facet: Crisp White (`#FFFFFF`).
  - Shaded underside facets: Cool Slate (`#E2E8F0` and `#CBD5E1`) creating instant depth without needing bitmap shadows.
  - Outer subtle orbital / compass tracker ring: Sky Blue (`#38BDF8` with 40–60% opacity) symbolizing goal alignment & application tracking.
- **Accent Layer (`spark`):**
  - Distinct 4-point dynamic diamond spark (`#F59E0B` Amber or `#38BDF8` Cyan) at the nose/trajectory point, representing autonomous AI search & match scoring.

### 2. Geometry & Safe Zones
- **Android Adaptive Icon (`108dp x 108dp`):**
  - Full canvas: `108dp x 108dp`.
  - Content bounding box: Strict `66dp x 66dp` centered at `(54, 54)` (bounding box `[21, 21]` to `[87, 87]`).
  - Guaranteed zero clipping on circle, squircle, or rounded-rect launcher masks.
- **iOS App Icon (`1024 x 1024`):**
  - Full bleed background to corners (iOS applies the squircle mask automatically).
  - Safe zone: Inner `800 x 800` centered area keeps glider and spark safely away from edge clipping.
- **Favicon (`16x16`, `32x32`, `48x48`):**
  - Simplified silhouette: High-contrast white glider + gold spark against `#2F5597` background, optimized pixel-grid alignment so edges remain razor-sharp at 16px.

---

## Proposed Changes

### Component 1: Master Vector & Compose Multiplatform (`frontend/core/design-system`)

#### [NEW] `frontend/core/design-system/src/commonMain/resources/drawable/ic_logo_brand.xml`
- Canonical multiplatform vector drawable.

#### [NEW] `frontend/core/design-system/src/commonMain/kotlin/.../widgets/BrandLogo.kt`
- Composable rendering the logo with configurable size and optional animation:
  - `animateGliderOffset`: Subtle hovering floating effect.
  - `animateSparkPulse`: Periodic shimmer/scale on the 4-point AI spark.

---

### Component 2: Android App Launcher Assets (`frontend/apps/android`)

#### [MODIFY] `frontend/apps/android/src/main/res/drawable/ic_launcher_background.xml`
- Replace Android green grid with `#2F5597` / `#1B365D` gradient.

#### [MODIFY] `frontend/apps/android/src/main/res/drawable-v24/ic_launcher_foreground.xml`
- New vector foreground containing the delta glider, orbital arc, and AI spark within `72dp` safe bounds.

#### [NEW] `frontend/apps/android/src/main/res/drawable-v33/ic_launcher_monochrome.xml`
- Single-color silhouette for Android 13+ Material You themed icons.

#### [MODIFY] `frontend/apps/android/src/main/res/mipmap-*/ic_launcher.png`
- Regenerate legacy launcher raster fallbacks.

---

### Component 3: iOS App Icon (`frontend/apps/ios`)

#### [MODIFY] `frontend/apps/ios/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json`
- Update Asset Catalog to support modern iOS 18 single 1024x1024 universal icon along with dark and tinted variants:
  - `app-icon-1024.png` (Standard / Light: `#2F5597` background + crisp white glider + gold spark)
  - `app-icon-1024-dark.png` (Dark: Deep `#0F172A` background + vibrant blue/cyan glider + gold spark)
  - `app-icon-1024-tinted.png` (Tinted: Grayscale silhouette that accepts iOS system tinting)

#### [NEW/MODIFY] `frontend/apps/ios/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`
- Exported 1024x1024 PNG with safe inner margins.

#### [SYNC] `frontend/apps/ios/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`
- Synchronize secondary asset catalog folder in the Xcode project so both project references stay aligned.

---

### Component 4: Web App & Favicon (`frontend/apps/web`)

#### [NEW] `frontend/apps/web/src/webMain/resources/favicon.ico`
- Multi-resolution ICO containing 16x16, 32x32, 48x48 mip levels.

#### [NEW] `frontend/apps/web/src/webMain/resources/favicon.svg`
- Vector favicon supporting dark mode via CSS `@media (prefers-color-scheme: dark)`.

#### [NEW] `frontend/apps/web/src/webMain/resources/apple-touch-icon.png`
- 180x180 PNG for mobile web home-screen bookmarks.

#### [MODIFY] `frontend/apps/web/src/webMain/resources/index.html`
- Update `<head>` with icon links.
- Replace generic spinning circle with SVG animated glider loader.

---

### Component 5: Desktop Icons (`frontend/apps/desktop`)

#### [NEW] `frontend/apps/desktop/src/jvmMain/resources/icons/icon-512.png`
- High-res PNG for desktop window decoration and Linux launcher.

#### [NEW] `frontend/apps/desktop/src/jvmMain/resources/icons/icon.icns` & `icon.ico`
- Package assets for macOS and Windows package distributions.

---

## Verification Plan

### Automated Checks
- `mise run dev:guards` — ensure newly added assets follow naming/permission rules.
- `mise run frontend:android:assemble` — verify Android adaptive icon resource compilation.
- `mise run frontend:desktop:jar` — verify desktop asset bundling.
- Validate PNG/ICO sizes and headers using image tools.

### Manual Verification
1. **Android Emulator/Device:** Inspect launcher icon on round launcher (Pixel) and squircle launcher (Samsung).
2. **iOS Simulator:** Build and run iOS app, verify icon displays cleanly on iOS home screen with no edge cutoff.
3. **Web Browser:** Load web app at `http://127.0.0.1:8000`, confirm favicon visibility on both light and dark browser tabs.
