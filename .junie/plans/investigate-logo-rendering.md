---
sessionId: session-261112-020807-10hj
---

# Plan: Fix inconsistent company logo rendering in Compose UI

**Status:** Completed  
**Date:** 2026-11-12  
**Target:** `frontend/core/widgets/.../CompanyLogoImage.kt`, `frontend/core/widgets/.../di/CoreWidgetsModule.kt`

---

### Overview

Fix the inconsistent display of company logos in the Android (and Desktop/Wasm) Compose UI. Hunter.io logos return successfully from `https://logos.hunter.io/{domain}` and are stored in Firestore with metadata (format, dimensions, shape), but many valid logos are silently rejected by the `isAcceptableSquareLogo()` quality gate in `CompanyLogoImage.kt`, causing the monogram fallback to be shown instead.

### Root Causes

**1. `isAcceptableSquareLogo()` rejects valid rectangular logos (CompanyLogoImage.kt:123-129)**

The quality gate requires:
- Both dimensions >= 64px (`MIN_LOGO_DIMENSION_PX = 64f`)
- Aspect ratio between 0.8-1.25 (`MIN_SQUARE_ASPECT_RATIO` / `MAX_SQUARE_ASPECT_RATIO`)

Hunter.io serves logos of varying sizes and shapes. The Python fetcher stores `logoShape` as RECTANGLE for non-square logos (e.g., Affirm: 1024x537, aspect ratio ~1.9). These valid logos are silently rejected, showing the monogram fallback instead. The `ContentScale.Crop` scaling mode already handles rectangular logos by cropping to fit the square container, so the aspect ratio check is redundant and harmful.

**2. `resolveCompanyLogoUrl()` fallback generates wrong URLs (CompanyLogoImage.kt:105-121)**

When `logoUrl` is null, the component generates a URL from the company name using aggressive normalization that strips corporate suffixes and non-alphanumeric characters. This produces different domains than `normalizeDomain()` in `tracker.py`, which uses a slug-based approach with aliases. The fallback can generate wrong URLs that 404.

**3. Silent error swallowing (CoreWidgetsModule.kt:138-148)**

The `DefaultImageLoadEventListener` is a no-op — errors are logged by the `CoilEventListenerBridge` via `DebugLogger` but not surfaced to the user or to structured logging, making debugging hard.

### Key Decisions

1. **Remove the aspect ratio check entirely.** The `ContentScale.Crop` mode already handles rectangular logos by cropping to the square container. The aspect ratio gate was designed to reject banner-style logos, but Hunter.io logos are all reasonable company logos that should render. The 64px minimum dimension threshold will be kept to filter out 1x1 placeholder images.
2. **Use Firestore `logoShape` metadata to pre-filter.** If `logoShape` is UNKNOWN or dimensions are 0, skip the load attempt entirely — these are known-bad records from the fetcher.
3. **Add diagnostic logging for rejected logos.** Log when `isAcceptableSquareLogo()` rejects a valid image so future issues are visible in logcat.
4. **Delete `resolveCompanyLogoUrl()` fallback entirely.** No one consumes this code but ourselves. Firestore `logoUrl` is authoritative after the logo-fetch and enrichment runs. If `logoUrl` is null, show the monogram fallback — don't try to guess the domain.
5. **Delete `resolveLinkedinUrl()` from `CompanySectorNormalizer`.** Hunter.io provides LinkedIn handles directly. The hardcoded slugs were already removed; the method now returns null. Delete it entirely.

### Proposed Changes

#### 1. Relax `isAcceptableSquareLogo()` to accept rectangular logos

**File:** `frontend/core/widgets/src/commonMain/kotlin/.../CompanyLogoImage.kt`

- Remove the aspect ratio check (lines 127-128). The `ContentScale.Crop` mode handles rectangular logos.
- Lower `MIN_LOGO_DIMENSION_PX` from 64 to 32 to accept smaller valid logos.
- Rename to `isValidLogoSize()` to reflect that it only checks dimensions, not shape.
- Add diagnostic logging when a logo is rejected (logging intrinsic size).

#### 2. Delete `resolveCompanyLogoUrl()` fallback

**File:** `frontend/core/widgets/src/commonMain/kotlin/.../CompanyLogoImage.kt`

- Delete `resolveCompanyLogoUrl()` method entirely.
- Remove the fallback call in `CompanyLogoImage` — if `logoUrl` is null, set `effectiveUrl` to null and show the monogram fallback.
- Remove the `COMPANY_CORPORATE_SUFFIXES` and related normalization constants.

#### 3. Delete `resolveLinkedinUrl()` from `CompanySectorNormalizer`

**File:** `records/parser/src/main/kotlin/.../CompanySectorNormalizer.kt`

- Delete `resolveLinkedinUrl()` method and all related code.
- Remove any comments referencing the deprecated method.

#### 4. Add diagnostic logging to `DefaultImageLoadEventListener`

**File:** `frontend/core/widgets/src/commonMain/kotlin/.../di/CoreWidgetsModule.kt`

- Update `DefaultImageLoadEventListener` to log errors via platform-specific logging (e.g., `android.util.Log` on Android) so failures are visible in logcat.

### Risks

1. **Rectangular logos may look cropped.** `ContentScale.Crop` will center-crop rectangular logos. This is standard behavior for company logos in square containers (e.g., Google, GitHub). If a logo has critical content at the edges, it may be cropped, but this is preferable to not showing the logo at all.
2. **Small logos (32px) may appear pixelated.** Lowering the threshold to 32px may show very small logos that look blurry when scaled up. Mitigation: the 32px threshold still filters out 1x1 placeholder images while allowing legitimate small logos.

# Delivery Steps

### ✓ Step 1: Relax `isAcceptableSquareLogo()`, delete fallbacks, and add diagnostic logging
Remove the aspect ratio check so rectangular logos from Hunter.io render correctly, and delete the deprecated fallback code.

- In `CompanyLogoImage.kt`: Remove the aspect ratio check from `isAcceptableSquareLogo()`. Rename to `isValidLogoSize()` and only check that both dimensions >= 32px (lowered from 64px).
- Delete `resolveCompanyLogoUrl()` method and related constants from `CompanyLogoImage.kt`. If `logoUrl` is null, show the monogram fallback.
- Delete `resolveLinkedinUrl()` method from `CompanySectorNormalizer.kt`.
- Add diagnostic logging when a logo is rejected (log intrinsic size via platform-specific logging).
- Update `DefaultImageLoadEventListener.onError()` in `CoreWidgetsModule.kt` to log the full error message and HTTP code via platform-specific logging so failures are visible in logcat.

### ✓ Step 2: Build and verify all logos render
Verify that the changes compile and all logos render correctly.

- Build and verify the frontend compiles (`./gradlew -p frontend:core:widgets:compileKotlin` or equivalent).
- Build and verify the records parser compiles (`./gradlew -p records:parser:compileKotlin` or equivalent).
- Run the Android app, open the companies list, and verify that rectangular logos (e.g., Affirm 1024x537) now render correctly.

### ✓ Step 3: Migrate plan to `.agents/plans/` per TEMPLATE.md
The plan has been migrated to `.agents/plans/2026-11-12/023100_fix-company-logo-rendering-compose-ui.md` and registered in `.agents/plans/README.md`.