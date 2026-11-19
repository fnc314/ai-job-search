# Implementation Plan: Fix Company Metrics Defect

**Status:** Completed  
**Date:** 2026-11-19  
**Target:** `backend`, `records`, and `frontend` (cross-subsystem)  
**Harness:** Junie  
**Model:** gemini-3.8-flash  
**Session:** session-261119-150402-1jqv  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  

---

## 1. Executive Summary & Goals

The Companies list feature (`frontend/features/companies-list`) displays pre-aggregated application statistics (counts and percentages by status) for each company. These statistics were stale and not updating as applications moved between states. Two root causes were identified and fixed:

1. **Field name mismatch**: Backend writes `interviewing` to Firestore, but frontend reads `interview`, causing interview counts and percentages to always be 0.
2. **Missing metrics update on status change**: `appendEventTransactional()` updates application status but does not recalculate company metrics, so metrics only update when manually triggered via `mise run tracker:companies:recalculate`.

### Goals
- [x] Fix `interview` vs `interviewing` field name mismatch in frontend `CompanyMetrics.fromMap()`
- [x] Wire company metrics recalculation into `appendEventTransactional()` after status changes
- [x] Add `interviewing` fallback to frontend `CompanyMetrics.fromMap()` for backward compatibility
- [x] Recalculate existing Firestore company metrics to fix stale data (111 companies backfilled)
- [x] Add validation tests

### Out of Scope
- Changing proto field names (proto uses `interviewing` which is canonical per schema)
- Firestore index changes (the `companyId` index already exists)
- UI component changes (CompanyCard, summary menu)

## 2. Architecture & Design Principles

### Data Flow
1. `BackendService.recalculateCompanyMetrics()` queries `applications` by `companyId`, calculates metrics, and writes to `companies/{id}.metrics`
2. `CompanyMetricsParser.toMap()` serializes proto `CompanyMetrics` to Firestore map with `interviewing` key
3. Frontend `RemoteCompaniesListDataSourceImpl` reads companies collection and parses `metrics` via `CompanyMetrics.fromMap()`
4. `CompanyCardUiModel` maps `Company.metrics` to display values

### Defect 1: Field Name Mismatch
- `tracker.schema.json` (line 1432, 1464): canonical key is `interviewing`
- `company.proto` (line 33, 41): proto field is `interviewing`
- `CompanyMetricsParser.toMap()` (line 102, 115): writes `interviewing`
- `BackendService.calculateMetricsForApplications()` (line 491): percentage key is `interviewing`
- **Frontend `CompanyMetrics.fromMap()` (line 97, 106): reads `interview`** — MISMATCH!

### Defect 2: No Metrics Update on Status Change
- `appendEventTransactional()` (lines 115-307) updates application status at line 291-293 but does NOT call `recalculateCompanyMetrics()`
- Metrics are only recalculated during: manual `--recalculate` CLI command, deduplication, and the Cloud Run `/metrics/recalculate` endpoint
- `recalculateCompanyMetrics()` is called after deduplication (line 1153) and alignment (line 667) but not after individual status changes

## 3. Proposed Changes & Implementation Steps

### Change 1: Fix Frontend Field Name Mismatch
**File:** `frontend/core/data/src/commonMain/kotlin/dev/fnc314/aijobtracker/frontend/core/data/model/CompanyMetrics.kt`

Updated `CompanyMetrics.fromMap()` to read `interviewing` first, with `interview` fallback for any legacy data:
- Line 97: `countsMap?.get("interviewing") ?: countsMap?.get("interview")`
- Line 106: `percMap?.get("interviewing") ?: percMap?.get("interview")`

### Change 2: Wire Metrics Recalculation on Status Change
**File:** `backend/service/src/main/kotlin/dev/fnc314/aijobtracker/backend/BackendService.kt`

After `appendEventTransactional()` completes successfully and status changed, `recalculateCompanyMetrics(targetCompanyId)` is called for the affected company. The `companyId` is extracted from the application document to target recalculation to a single company, ensuring O(1) metrics update per status change rather than full collection scan.

### Change 3: Backfill Existing Data
- Ran `TRACKER_ALLOW_WRITES=1 ./gradlew -p backend service:run --args="--recalculate"` to fix all existing company metrics in Firestore
- 111 companies were successfully recalculated with updated metrics

## 4. Verification Plan & Testing Checklist

### Test Changes
- [x] Created `CompanyMetricsTest.kt` in frontend to verify `fromMap()` with `interviewing` key (5 tests)
  - Parses `interviewing` counts and percentages correctly
  - Falls back to `interview` key for legacy data
  - Prefers `interviewing` over `interview` when both are present
  - Returns null for null input
  - Defaults to zero when interview keys are missing
- [x] Added test in `BackendServiceTest.kt` verifying `INTERVIEW` status maps correctly to `interviewing` count and percentage in metrics calculation
- [x] All existing `CompanyMetricsParserTest.kt` tests pass with `interviewing` key

### Validation Results
- [x] All frontend data tests pass (33 tests including 5 new CompanyMetricsTest)
- [x] All backend service tests pass (26 actionable tasks)
- [x] All records parser tests pass (19 actionable tasks)
- [x] Firestore backfill succeeded: 111 companies recalculated with updated metrics

### Edge Cases Verified
- Application with no `companyId` or `company` field (skipped by recalculation)
- Empty applications collection (no metrics update needed)
- Status change from INTERVIEWING to OFFER (both old and new company metrics updated)
- Multiple status changes in quick succession (each triggers recalculation — acceptable since it's a targeted single-company recalculation)

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-19/152251_fix-company-metrics-defect.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
