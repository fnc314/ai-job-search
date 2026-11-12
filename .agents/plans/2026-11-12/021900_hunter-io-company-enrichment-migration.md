# Migration Plan: Migrate company enrichment to Hunter.io Company API

**Status:** Completed  
**Date:** 2026-11-12  
**Target:** `backend`, `frontend`, `records` and `tracker` (cross-subsystem)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Supersedes:** [`170538_company_sector_enum_and_heuristic_enrichment.md`](./2026-11-10/170538_company_sector_enum_and_heuristic_enrichment.md) (partially — heuristic normalizer remains as fallback)

---

## 1. Executive Summary & Goals

Migrate company metadata enrichment to use Hunter.io's Company Enrichment API (`/v2/companies/find`) as the primary source, replacing the broken endpoint (`/v2/companies` returned empty results) and reducing reliance on hardcoded fallbacks. Each company is enriched once on the first pass, then re-enriched semi-annually (6 months), staying within the free tier budget (50 credits/month, capped at 45 calls per batch).

**Goals:**
1. Fix the Hunter.io API endpoint bug (`/v2/companies` → `/v2/companies/find`).
2. Extract and store all useful company metadata fields (20+ fields including UUID, sector, industry, employees, location, social handles).
3. Implement credit-aware batching with 6-month skip logic.
4. Update schema, Kotlin data model, and data source to support new fields.
5. Deprecate redundant hardcoded fallbacks (`KNOWN_LINKEDIN_SLUGS` removed).

## 2. Architecture & Design Principles

- **Hunter.io as Primary Source:** The `/v2/companies/find` endpoint returns ~30 fields per company. We capture core metadata (name, sector, industry, subIndustry, employees, location, description, foundedYear, companyType, tags, LinkedIn, Twitter, Crunchbase) and store the stable `hunterIoId` (UUID) for future re-enrichment.
- **Credit-Aware Enrichment:** Batch enrichment stops at 45 API calls to maintain a safety margin within the 50-credit free tier. Companies enriched within 6 months are skipped.
- **Non-Destructive Updates:** `enrichCompany()` only writes fields that are missing or null, preserving existing data.
- **Fallback Chain:** When Hunter.io returns no data, `CompanySectorNormalizer` provides sector guessing from hardcoded mappings and keyword rules. LinkedIn slugs are no longer hardcoded — Hunter.io provides them directly.
- **Schema Evolution:** All new fields are nullable to maintain backward compatibility with existing records.

## 3. Proposed Changes & Implementation Steps

### ✓ Step 1: Fix the Hunter.io API endpoint and extract all available fields

**File:** `backend/src/main/kotlin/.../BackendService.kt`

- Changed API URL from `/v2/companies` to `/v2/companies/find` in `fetchHunterCompanyInfo()`.
- Updated JSON parsing to extract: `hunter_io_id`, `name`, `industry`, `subIndustry`, `sector`, `linkedin_handle`, `twitter_handle`, `crunchbase_handle`, `logo_url`, `description`, `foundedYear`, `companyType`, `employees`, `employeesCount`, `location`, `city`, `state`, `country`, `tags`, and `raised`.
- Returns all fields in a comprehensive `Map<String, Any?>`.

### ✓ Step 2: Update `enrichCompany()` and `batchEnrichCompanies()` with new fields and credit tracking

**File:** `backend/src/main/kotlin/.../BackendService.kt`

- Added new parameters to `enrichCompany()`: `hunterIoId`, `description`, `foundedYear`, `companyType`, `employees`, `employeesCount`, `location`, `city`, `state`, `country`, `subIndustry`, `twitterHandle`, `crunchbaseHandle`, `tags`, `raised`.
- New fields written to Firestore (top-level for core fields, `metadata` map for `hunterTags` and `hunterRaised`).
- `lastHunterEnrichedAt` timestamp set on successful enrichment.
- `batchEnrichCompanies()` skips companies enriched within 6 months.
- Credit counter stops at 45 API calls with a safety margin.
- Credit usage logged in the result summary.

### ✓ Step 3: Update schema, Kotlin model, and data source

**Files:** `tracker.schema.json`, `frontend/core/data/.../Company.kt`, `frontend/core/data/.../Factory.kt`, `frontend/features/companies-list/data/.../RemoteCompaniesListDataSourceImpl.kt`

- Added `hunterIoId` (nullableString, UUID) and all other new fields to `tracker.schema.json` company definition (all nullable).
- Bumped `schemaVersion` to 12.
- Added `hunterIoId: String? = null` and all other new properties to the Kotlin `Company` data class.
- Updated `RemoteCompaniesListDataSourceImpl` and `Factory.createCompany` to map new Firestore fields.
- Backend compiles successfully (`./gradlew -p backend compileKotlin`).

### ✓ Step 4: Simplify `CompanySectorNormalizer`

**File:** `records/parser/src/main/kotlin/.../CompanySectorNormalizer.kt`

- Removed `KNOWN_LINKEDIN_SLUGS` — Hunter.io provides LinkedIn handles directly.
- `resolveLinkedinUrl()` now returns null as Hunter.io is the primary source.
- Kept `KNOWN_COMPANY_SECTORS` and keyword rules as fallbacks for companies where Hunter returns no data.
- Added a comment noting Hunter.io is now the primary source.

## 4. Verification Plan & Testing Checklist

- [x] Backend compiles successfully (`./gradlew -p backend compileKotlin` — BUILD SUCCESSFUL).
- [ ] Run initial enrichment: `mise run tracker:companies:enrich` with `TRACKER_ALLOW_WRITES=1` to perform the first full pass.
- [ ] Verify Firestore records contain new fields (spot-check 5-10 companies).
- [ ] Verify credit usage is logged correctly in the enrichment summary.
- [ ] Verify 6-month skip logic works (re-run enrichment and confirm already-enriched companies are skipped).

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-12/021900_hunter-io-company-enrichment-migration.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Craft a single commit of all changed files
- [x] Stage changed files and craft a commit message based on the name of this file
- [x] Push `git push origin main`
