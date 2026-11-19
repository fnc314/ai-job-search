---
sessionId: session-261119-150402-1jqv
---

# Requirements

### Overview & Goals

The Companies list feature (`frontend/features/companies-list`) displays pre-aggregated application statistics (counts and percentages by status) for each company. These statistics are stale and not updating as applications move between states. Two root causes were identified:

1. **Field name mismatch**: Backend writes `interviewing` to Firestore, but frontend reads `interview`, causing interview counts and percentages to always be 0.
2. **Missing metrics update on status change**: `appendEventTransactional()` updates application status but does not recalculate company metrics, so metrics only update when manually triggered via `mise run tracker:companies:recalculate`.

### Scope

**In Scope:**
- Fix `interview` vs `interviewing` field name mismatch in frontend `CompanyMetrics.fromMap()`
- Wire company metrics recalculation into `appendEventTransactional()` after status changes
- Add `interviewing` fallback to frontend `CompanyMetrics.fromMap()` for backward compatibility
- Recalculate existing Firestore company metrics to fix stale data
- Add validation tests

**Out of Scope:**
- Changing proto field names (proto uses `interviewing` which is canonical per schema)
- Firestore index changes (the `companyId` index already exists)
- UI component changes (CompanyCard, summary menu)

# Technical Design

### Current Implementation

**Data Flow:**
1. `BackendService.recalculateCompanyMetrics()` queries `applications` by `companyId`, calculates metrics, and writes to `companies/{id}.metrics`
2. `CompanyMetricsParser.toMap()` serializes proto `CompanyMetrics` to Firestore map with `interviewing` key
3. Frontend `RemoteCompaniesListDataSourceImpl` reads companies collection and parses `metrics` via `CompanyMetrics.fromMap()`
4. `CompanyCardUiModel` maps `Company.metrics` to display values

**Defect 1 - Field Name Mismatch:**
- `tracker.schema.json` (line 1432, 1464): canonical key is `interviewing`
- `company.proto` (line 33, 41): proto field is `interviewing`
- `CompanyMetricsParser.toMap()` (line 102, 115): writes `interviewing`
- `BackendService.calculateMetricsForApplications()` (line 491): percentage key is `interviewing`
- **Frontend `CompanyMetrics.fromMap()` (line 97, 106): reads `interview`** — MISMATCH!

**Defect 2 - No Metrics Update on Status Change:**
- `appendEventTransactional()` (lines 115-307) updates application status at line 291-293 but does NOT call `recalculateCompanyMetrics()`
- Metrics are only recalculated during: manual `--recalculate` CLI command, deduplication, and the Cloud Run `/metrics/recalculate` endpoint
- `recalculateCompanyMetrics()` is called after deduplication (line 1153) and alignment (line 667) but not after individual status changes

### Proposed Changes

**1. Fix Frontend Field Name Mismatch** (`frontend/core/data/CompanyMetrics.kt`)
- Update `CompanyMetrics.fromMap()` to read `interviewing` (with `interview` fallback for any legacy data):
  - Line 97: `countsMap?.get("interviewing") ?: countsMap?.get("interview")`
  - Line 106: `percMap?.get("interviewing") ?: percMap?.get("interview")`

**2. Wire Metrics Recalculation on Status Change** (`backend/service/BackendService.kt`)
- After `appendEventTransactional()` completes successfully and status changed, call `recalculateCompanyMetrics(targetCompanyId)` for the affected company
- Extract `companyId` from the application document and pass to targeted recalculation
- This ensures O(1) metrics update per status change rather than full collection scan

**3. Backfill Existing Data**
- Run `mise run tracker:companies:recalculate` to fix all existing company metrics in Firestore

### Files to Modify
- `frontend/core/data/src/commonMain/kotlin/dev/fnc314/aijobtracker/frontend/core/data/model/CompanyMetrics.kt` — fix field name
- `backend/service/src/main/kotlin/dev/fnc314/aijobtracker/backend/BackendService.kt` — wire metrics update
- `backend/service/src/test/kotlin/dev/fnc314/aijobtracker/backend/BackendServiceTest.kt` — add test
- `frontend/core/data/src/commonTest/kotlin/dev/fnc314/aijobtracker/frontend/core/data/model/CompanyMetricsTest.kt` (new) — add parsing test

# Testing

### Key Scenarios
- Frontend `CompanyMetrics.fromMap()` correctly parses `interviewing` from Firestore map
- Frontend `CompanyMetrics.fromMap()` falls back to `interview` for legacy data
- After status change event, company metrics are recalculated with correct counts
- Interview count reflects actual INTERVIEWING status applications
- Percentages sum to ~1.0 across all statuses

### Edge Cases
- Application with no `companyId` or `company` field (skipped by recalculation)
- Empty applications collection (no metrics update needed)
- Status change from INTERVIEWING to OFFER (both old and new company metrics updated)
- Multiple status changes in quick succession (each triggers recalculation — acceptable since it's a targeted single-company recalculation)

### Test Changes
- Add `CompanyMetricsTest.kt` in frontend to verify `fromMap()` with `interviewing` key
- Add test in `BackendServiceTest.kt` to verify metrics recalculation after `appendEventTransactional()` with status change
- Verify existing `CompanyMetricsParserTest.kt` still passes with `interviewing` key

# Delivery Steps

### ✓ Step 1: Fix frontend metrics field name mismatch
Update `CompanyMetrics.fromMap()` to read `interviewing` (with `interview` fallback) so interview counts and percentages are correctly parsed from Firestore.
- Modify `frontend/core/data/.../CompanyMetrics.kt` line 97 and 106 to check `interviewing` first, then fall back to `interview`
- Create `CompanyMetricsTest.kt` in frontend/core/data tests to verify parsing with both `interviewing` and `interview` keys
- Run `./gradlew :frontend:core:data:test` to validate

### * Step 2: Wire metrics recalculation on status change
After `appendEventTransactional()` updates application status, recalculate metrics for the affected company.
- In `BackendService.kt`, after the transaction completes in `appendEventTransactional()`, check if `statusTo` was set and call `recalculateCompanyMetrics(targetCompanyId)` with the application's company ID
- Extract `companyId` from `appSnap` to target the recalculation to a single company (efficient)
- Add test in `BackendServiceTest.kt` to verify metrics are updated after status change event
- Run `./gradlew :backend:service:test` to validate

###   Step 3: Backfill existing Firestore metrics and validate
Recalculate all existing company metrics in Firestore to fix stale data, and run full validation.
- Run `mise run tracker:companies:recalculate` to backfill all company metrics
- Run `mise run records:test` and `mise run frontend:test` to ensure no regressions
- Verify the companies list UI shows correct interview counts and percentages