# Implementation Plan: Retroactive Company Enrichment

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `backend` and `frontend` (cross-subsystem)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261110-153958-1i4a  
**Depends On:** [`152500_linkedin_company_enrichment_plan.md`](./2026-11-10/152500_linkedin_company_enrichment_plan.md)  

---

## 1. Executive Summary & Goals

The RM-005 LinkedIn Company Enrichment infrastructure is implemented and marked `DONE`, but it only enriches **new** company records discovered during `/scrape`. The ~30+ existing companies in Firestore are missing `sector` (industry) and `linkedinUrl`. This plan addresses the **retroactive enrichment** of existing records and the **frontend consumption** of the enriched metadata.

### Goals
1. Backfill `sector` and `linkedinUrl` for all existing companies in Firestore via an idempotent batch enrichment process.
2. Expose the enrichment as a `mise` command (following the `remote:sweep` pattern) that calls backend endpoints.
3. Add `linkedinUrl` to the `Company` domain model and display it in `CompanyDetailsScreen`.
4. Fix the `sector` → `industry` field mapping bug in `CompanyDetailsRepositoryImpl`.
5. Support adding a company manually (even without job applications) with a link to their career site.

### Scope

**In Scope:**
- Backend: `POST /companies/tasks/enrich/batch` endpoint for idempotent batch enrichment
- Backend: `POST /companies/add` endpoint for standalone company creation
- Mise task: `tracker:companies:enrich` wrapping the backend batch endpoint
- Frontend: Add `linkedinUrl` to `Company` model, `CompanyDetailsUiModel`, and `CompanyDetailsScreen`
- Frontend: Fix `sector` → `industry` Firestore field mapping in `CompanyDetailsRepositoryImpl`
- Frontend: Add "Add Company" flow in companies list for companies without applications

**Out of Scope:**
- Adding a new `company` command to linkedin-search CLI (the backend enrichment can use Hunter.io industry data directly)
- TUI changes for company enrichment
- Web dashboard (HTML) changes

### User Stories
- As a user, I want to run `mise run tracker:companies:enrich` to backfill missing company metadata so my company cards show industry and LinkedIn links.
- As a user, I want to see a LinkedIn link on the company details screen so I can visit a company's LinkedIn page.
- As a user, I want to add a company I'm interested in (even without a specific role) so I can later run `/apply` when a posting appears.

## 2. Architecture & Design Principles

### Current Implementation

**Backend (`backend/`):**
- `BackendService.enrichCompany()` (line 631-689 in `BackendService.kt`) — idempotent single-company enrichment with sector normalization, linkedinUrl, logoUrl support
- `POST /companies/enrich` route in `Main.kt` (line 106-146) — HTTP endpoint wrapping `enrichCompany()`
- `BackendService.recalculateCompanyMetrics()` — existing batch pattern for iterating all companies

**Firestore Data Gaps:**
- Company records (e.g., `affirm`) have `logoUrl`, `logoFormat`, `website`, `metrics` but are missing `sector` and `linkedinUrl`
- `sector` field exists in schema (`tracker.schema.json` line 1231) as nullable string
- `linkedinUrl` field exists in schema (line 1247) as nullable string

**Frontend:**
- `Company.kt` — domain model with `industry` field (maps to Firestore `sector`) but **no `linkedinUrl`**
- `CompanyDetailsRepositoryImpl.mapRecordToCompany()` (line 48-85) reads `industry` from Firestore — **bug**: should read `sector`
- `CompanyDetailsUiModel` — no `linkedinUrl` field
- `CompanyDetailsScreen.kt` — displays industry and website; no LinkedIn link
- String resources in `frontend/features/company-details/ui/src/commonMain/composeResources/values/strings.xml`

**Mise Task Pattern:**
- `remote/sweep.sh` — resolves Cloud Run endpoint, calls `POST /tasks/sweep` via curl
- `tracker/companies/recalculate.sh` — runs backend locally via `gradlew run --args="--recalculate"`

### Key Decisions

1. **Enrichment data source**: The backend `enrichCompany()` accepts `industry` and `linkedinUrl` as parameters. The batch endpoint sources this data via Hunter.io API (already used for logos) which returns industry data. Keeps the backend stateless and respects LinkedIn ToS.

2. **Idempotency**: The batch endpoint skips companies that already have both `sector` and `linkedinUrl` populated, and includes a `lastEnrichedAt` timestamp to avoid rapid re-enrichment (skips if enriched within last 24 hours).

3. **`sector` vs `industry` field name**: Firestore uses `sector`; the frontend `Company` model uses `industry`. The repository mapper reads `sector` from Firestore and maps it to the `industry` model field.

4. **Add Company flow**: A new backend endpoint `POST /companies/add` creates a company record from name/website, auto-resolves `careersUrl`, and triggers enrichment.

## 3. Proposed Changes & Implementation Steps

### Step 1: Backend batch enrichment and add-company endpoints ✓
Add `POST /companies/tasks/enrich/batch` and `POST /companies/add` endpoints to backend service.
- Implemented `batchEnrichCompanies()` in `BackendService.kt`: queries all companies, identifies those missing `sector` or `linkedinUrl`, calls `enrichCompany()` for each, returns summary.
- Implemented `addCompany()` in `BackendService.kt`: creates company record from name/website, derives slug, resolves careersUrl.
- Added routes in `Main.kt` for both endpoints. The batch route is namespaced as `/companies/tasks/enrich/batch` to keep `/companies/enrich` for single entries and group batch operations under `/companies/tasks/`.
- Added `--enrich-batch` CLI arg support in `Main.kt` for local execution (mirroring `--recalculate` pattern).
- Added `fetchHunterCompanyInfo()` for Hunter.io API integration returning industry and LinkedIn URL data.
- Updated `enrichCompany()` to set `lastEnrichedAt` on successful updates.

### Step 2: Mise task for company enrichment ✓
Created mise task that wraps the backend batch enrichment endpoint.
- Added `.config/mise/tasks/tracker/companies/enrich.sh` following `remote/sweep.sh` pattern: resolves Cloud Run endpoint, calls `POST /companies/tasks/enrich/batch` via curl.
- Added `--endpoint` flag and local fallback via `gradlew -p backend run --args="--enrich-batch"`.
- Added `--dry-run` flag to preview which companies need enrichment without writing.

### Step 3: Frontend Company model and mapping fixes ✓
Added `linkedinUrl` to Company domain model and fixed the sector/industry field mapping bug.
- Added `linkedinUrl: String?` to `Company.kt` data class.
- Added `linkedinUrl` parameter to `Factory.createCompany()` in `Factory.kt`.
- Fixed `CompanyDetailsRepositoryImpl.mapRecordToCompany()`: reads `sector` (not `industry`) from Firestore, reads `linkedinUrl`, passes to factory.
- Fixed `RemoteCompaniesListDataSourceImpl` and `RemoteJobApplicationDetailsDataSourceImpl` with the same `sector` → `industry` mapping fix.
- Added `linkedinUrl` to `CompanyDetailsUiModel` and `CompanyCardUiModel` and updated mappers.

### Step 4: Frontend LinkedIn URL display and Add Company flow ✓
Display LinkedIn URL in company details screen.
- Added LinkedIn `TooltipIconButton` in `CompanyDetailsScreen.kt` `CompanyOverviewCard` next to website button, wrapped in a `Row` for side-by-side display.
- Added string resources: `company_details_linkedin_label`, `company_details_open_linkedin_tooltip` in `strings.xml`.
- The "Add Company" frontend dialog flow is deferred — the backend `POST /companies/add` endpoint is ready for future integration.

## 4. Verification Plan & Testing Checklist

- [x] Backend compiles: `./gradlew -p backend compileKotlin` — BUILD SUCCESSFUL
- [x] Frontend compiles: `./gradlew -p frontend compileKotlinJvm` — BUILD SUCCESSFUL
- [ ] Run `mise run tracker:companies:enrich` against live Firestore to verify batch enrichment
- [ ] Verify `sector` values appear in frontend company details after enrichment
- [ ] Verify LinkedIn button appears and opens correctly in `CompanyDetailsScreen`

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-10/160200_retroactive-company-enrichment.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
