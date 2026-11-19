# Implementation Plan: Company Logo Sub-Object Refactoring and Metrics Calculation Fix

**Status:** Completed  
**Date:** 2026-11-19  
**Target:** `records`, `backend`, `frontend`, and Firestore (cross-subsystem)  
**Harness:** Antigravity  
**Model:** Gemini 3.8 Flash  
**Session:** ce817f4f-f31e-400f-af06-101b422e01de  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  

---

## 1. Executive Summary & Goals

This plan resolves two intertwined issues:
1. **Defective Company Metrics Aggregation**: Applications in canonical `SUBMITTED`, `INTERVIEWING`, `OFFER`, etc., states are improperly categorized (defaulting to `draft++`) during metrics recalculation in `BackendService.calculateMetricsForApplications()`. In particular, Amazon's application (`amazon--software-development-engineer-app-performance-mobile-app-foundations`) with `status: "SUBMITTED"` is counted as `draft`. Additionally, Gradle execution flags in company-related mise tasks targeted `:run` instead of `:service:run`, breaking `mise run tracker:companies:recalculate`.
2. **Company Logo Data Model Refactoring**: Existing company records represent logo metadata across disjoint flat attributes (`logoUrl`, `logoFormat`, `logoWidth`, `logoHeight`, `logoShape`, `logoFetchedAt`). This refactors company records in Firestore, schemas, protos, and models to bundle logo metadata into a unified structured object:
   ```json
   "logo": {
     "url": "https://logos.hunter.io/amazon.com",
     "format": "AVIF",
     "height": 0,
     "width": 0,
     "fetchedAt": "2026-11-19T05:27:16Z"
   }
   ```
   with backward-compatible support for legacy flat fields across all reading surfaces, followed by an in-place Firestore backfill.

### Goals
- [x] Fix status mapping in `BackendService.calculateMetricsForApplications()` to recognize canonical and legacy statuses across `draft`, `submitted`, `interviewing`, `offer`, and `rejected`.
- [x] Fix mise tasks under `.config/mise/tasks/tracker/companies/` to invoke `:service:run`.
- [x] Trigger company metrics recalculation in `UpdateStatusCommand` (and CLI flows) upon application status change.
- [x] Update `tracker.schema.json` to define `logo` object schema while maintaining backward compatibility for legacy flat keys.
- [x] Update `records/proto/tracker/company.proto` to include structured `Logo` message.
- [x] Update frontend models (`Company.kt`, `RemoteCompaniesListDataSourceImpl.kt`, `CompanyDetailsRepositoryImpl.kt`, `RemoteJobApplicationDetailsDataSourceImpl.kt`) to parse `logo: { ... }` with fallback to flat fields.
- [x] Update `BackendService.kt` to write structured `logo` objects during company enrichment, creation, and deduplication.
- [x] Update `firestore.rules` to permit and validate the `logo` map.
- [x] Create a migration command/script to bundle flat `logo*` attributes into `logo: { ... }` across all existing Firestore company documents.
- [x] Recalculate metrics across all companies, verifying `amazon` and others reflect accurate `SUBMITTED` counts.

---

## 2. Architecture & Design Principles

### Single Source of Truth
- `tracker.schema.json` is the authority for Firestore document schemas. The `logo` property will be defined under `Company` properties as an object with `url`, `format`, `height`, `width`, `fetchedAt`.
- Enums (`format`: `AVIF`, `WEBP`, `PNG`, `SVG`, `UNKNOWN`) are validated and serialized consistently.

### Robust Funnel Status Bucketing
- Status classification in `BackendService.calculateMetricsForApplications()` must align with `StatusNormalizer` and canonical funnel states:
  - **Draft**: `DRAFTING`, `DRAFTED`, `RANKED`, `LEAD`, `DRAFT`
  - **Submitted**: `SUBMITTED`, `APPLIED`, `NO_RESPONSE`
  - **Interviewing**: `INTERVIEWING`, `INTERVIEW`, `RECRUITER_SCREENED`
  - **Offer**: `OFFER`, `OFFER_EXPECTED`, `NEGOTIATING_PRE_OFFER`, `NEGOTIATING_POST_OFFER`, `HIRED`, `OFFER_DECLINED`
  - **Rejected**: `REJECTED`, `APPLICATION_WITHDRAWN`

### Backward-Compatible Migration
- Frontend data sources must accept both nested `logo: { ... }` and flat `logoUrl`, `logoFormat`, etc., preventing runtime failures during and after migration.

---

## 3. Proposed Changes & Implementation Steps

### Phase 1: Fix Status Bucketing & Task Invocations
1. **`BackendService.kt`**: Update `calculateMetricsForApplications()` status categorization.
2. **`BackendServiceTest.kt`**: Add test coverage for `SUBMITTED`, `INTERVIEWING`, `RECRUITER_SCREENED`, `OFFER`, `DRAFTED`, `RANKED`.
3. **Mise tasks**: Update `:service:run` in `.config/mise/tasks/tracker/companies/recalculate.zsh`, `deduplicate.zsh`, `audit.zsh`, `enrich.zsh`, `migrate-potentials.zsh`.
4. **`UpdateStatusCommand.kt`**: Wire company metrics recalculation after status update.

### Phase 2: Schema & Proto Updates
1. **`tracker.schema.json`**: Add `logo` object definition to `Company` definition with required/optional properties.
2. **`records/proto/src/main/proto/tracker/company.proto`**: Add `Logo` message and `Logo logo = 16;`. Recompile proto.

### Phase 3: Frontend Data Source & Model Support
1. **`Company.kt` & `Factory.kt`**: Add `logo` sub-model or retain convenient accessors while mapping `logo.url`, `logo.format`, `logo.width`, `logo.height`, `logo.fetchedAt`.
2. **`RemoteCompaniesListDataSourceImpl.kt`**: Read from `record.fields["logo"] as? Map<*, *>` first, falling back to flat fields.
3. **`CompanyDetailsRepositoryImpl.kt`**: Read from `record.fields["logo"] as? Map<*, *>` first, falling back to flat fields.
4. **`RemoteJobApplicationDetailsDataSourceImpl.kt`**: Read from `record.fields["logo"] as? Map<*, *>` first, falling back to flat fields.

### Phase 4: Backend Writing & Security Rules
1. **`firestore.rules`**: Add rule validation for `logo` map in `companies` collection.
2. **`BackendService.kt`**: Update `addCompany()`, `batchEnrichCompanies()`, and `deduplicateCompanies()` to write `logo: { ... }`.
3. **Migration Routine**: Add `--migrate-company-logos` to `BackendService.kt` / `Main.kt` and run with `TRACKER_ALLOW_WRITES=1`.

### Phase 5: Verification & Recalculation
1. Run `--migrate-company-logos` across Firestore.
2. Run `mise run tracker:companies:recalculate` across all companies.
3. Verify `amazon` reflects `counts: { submitted: 1, total: 1 }` and `logo: { url: ..., format: "AVIF", ... }`.
4. Run full test suite (`dev:test`, `frontend:test`, `records:test`).

---

## 4. Verification Plan & Testing Checklist

- [x] Unit tests pass: `./gradlew :backend:service:test`
- [x] Unit tests pass: `./gradlew -p frontend :features-companies-list-data:jvmTest :features-company-details-data:jvmTest :features-job-application-details-data:jvmTest`
- [x] Full dev tests pass: `mise run dev:test`
- [x] Dev guards pass: `mise run dev:guards`
- [x] `amazon` metrics in Firestore show `submitted = 1` and `total = 1`.
- [x] `amazon` company doc in Firestore contains bundled `logo` object.

---

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-19/171800_company-logo-refactor-and-metrics-calculation-fix.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
