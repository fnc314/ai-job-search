# Implementation Plan: Fix inconsistent company logo rendering in Compose UI

**Status:** Completed  
**Date:** 2026-11-12  
**Target:** `frontend` and `records` (cross-subsystem)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261112-020807-10hj  
**Supersedes:** [`220303_rm-009-unify-top-level-card-and-company-dedup.md`](./2026-11-11/220303_rm-009-unify-top-level-card-and-company-dedup.md) (partially — logo rendering fix)  

---

## 1. Executive Summary & Goals

Fix the inconsistent display of company logos in the Android (and Desktop/Wasm) Compose UI. Hunter.io logos return successfully from `https://logos.hunter.io/{domain}` and are stored in Firestore with metadata (format, dimensions, shape), but many valid logos are silently rejected by the `isAcceptableSquareLogo()` quality gate in `CompanyLogoImage.kt`, causing the monogram fallback to be shown instead.

**Goals:**
1. Remove the aspect ratio check from `isAcceptableSquareLogo()` so rectangular logos (e.g., Affirm 1024×537) render correctly via `ContentScale.Crop`.
2. Lower minimum dimension threshold from 64px to 32px to accept smaller valid logos.
3. Delete the `resolveCompanyLogoUrl()` fallback entirely — Firestore `logoUrl` is authoritative.
4. Delete `resolveLinkedinUrl()` from `CompanySectorNormalizer` — Hunter.io provides LinkedIn handles directly.
5. Add diagnostic logging for image load failures so issues are visible in logcat.

## 2. Architecture & Design Principles

- **ContentScale.Crop handles rectangular logos:** The Compose `AsyncImage` already uses `ContentScale.Crop` which center-crops rectangular logos to fit square containers. The aspect ratio gate was redundant and harmful.
- **Firestore logoUrl is authoritative:** After logo-fetch and enrichment runs, `logoUrl` in Firestore is the single source of truth. No fallback URL generation is needed.
- **Hunter.io as primary source:** Hunter.io provides LinkedIn handles, logo URLs, and company metadata directly. Hardcoded fallbacks are no longer needed.
- **Diagnostic visibility:** Image load errors should be logged with HTTP status codes for debugging, not silently swallowed.

## 3. Proposed Changes & Implementation Steps

### ✓ Step 1: Relax `isAcceptableSquareLogo()`, delete fallbacks, and add diagnostic logging

**File:** `frontend/core/widgets/src/commonMain/kotlin/.../CompanyLogoImage.kt`

- Removed aspect ratio check from `isAcceptableSquareLogo()`. Renamed to `isValidLogoSize()` — only checks that both dimensions >= 32px (lowered from 64px).
- Deleted `resolveCompanyLogoUrl()` method and related `COMPANY_CORPORATE_SUFFIXES` constants.
- If `logoUrl` is null, `effectiveUrl` is null and monogram fallback is shown.
- Added diagnostic logging when a logo is rejected (logs intrinsic size via `println`).

**File:** `records/parser/src/main/kotlin/.../CompanySectorNormalizer.kt`

- Deleted `resolveLinkedinUrl()` method and its KDoc.

**File:** `backend/src/main/kotlin/.../BackendService.kt`

- Removed calls to `CompanySectorNormalizer.resolveLinkedinUrl()` — Hunter.io provides LinkedIn handles directly.

**File:** `records/parser/src/test/kotlin/.../CompanySectorNormalizerTest.kt`

- Removed `testLinkedinUrlResolution` test.

**File:** `frontend/core/widgets/src/commonMain/kotlin/.../di/CoreWidgetsModule.kt`

- Updated `DefaultImageLoadEventListener.onError()` to log full error message and HTTP status code via `println` so failures are visible in logcat.

## 4. Verification Plan & Testing Checklist

- [x] Frontend compiles successfully (`:frontend:core-widgets:compileKotlinJvm` — BUILD SUCCESSFUL).
- [x] Records parser compiles successfully (`:records:records-parser:compileKotlin` — BUILD SUCCESSFUL).
- [x] Backend compiles successfully (`:backend:compileKotlin` — BUILD SUCCESSFUL with unrelated warnings).
- [ ] Run Android app, open companies list, and verify rectangular logos (e.g., Affirm 1024×537) render correctly.

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-12/023100_fix-company-logo-rendering-compose-ui.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
