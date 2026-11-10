# Implementation Plan: RM-005 Company Enrichment Backfill Execution

**Status:** Proposed  
**Date:** 2026-11-10  
**Target:** `backend/`, `mise run tracker:companies:enrich`, Firestore `companies` collection (`job-search-fnc314-dev`)  
**Evaluates:** Execution of RM-005 Company Enrichment backfill  
**Schema Authority:** `tracker.schema.json` v10 (`$defs/company`)  

---

## 1. Goal Description & Investigation Findings

An investigation into Firestore (`job-search-fnc314-dev`) was performed to determine if the RM-005 company enrichment backfill was previously executed:
- **Total companies in Firestore:** 94
- **Companies with `sector`:** 0
- **Companies with `linkedinUrl`:** 0
- **Companies missing both fields:** 94 (100% of existing companies)

When the dry run was executed (`mise run tracker:companies:enrich --dry-run`), it reported:
```text
Batch enrichment complete: {status=SUCCESS, enriched=0, skipped=94, errors=0}
```

### Why it was skipped:
In `BackendService.kt` (`fetchHunterCompanyInfo`), the batch enrichment routine queries `api.hunter.io/v2/companies?domain=...&api_key=$HUNTER_API_KEY`.
Because `HUNTER_API_KEY` is not configured in `.env.local` or the environment, `fetchHunterCompanyInfo` returned `emptyMap()`, and without industry or LinkedIn URL data returned, the enrichment step updated 0 records.

Furthermore, `scripts/tracker.py` contains a working logo fetcher using Hunter's public unauthenticated logo CDN (`logos.hunter.io/{domain}`), but logo metadata alone does not supply `sector` or `linkedinUrl`.

---

## 2. Proposed Solution & Execution Strategy

To execute the enrichment backfill effectively for the 94 companies:

1. **Leverage Known Company Domain & Sector Map:**
   - Seed known standard industry/sector classifications for existing companies (e.g. `Affirm` -> `FinTech`, `Anthropic` -> `Technology`, `American Eagle Outfitters` -> `Retail`, `Vanguard` -> `FinTech / Financial Services`, etc.).
   - Support deriving the company's official LinkedIn page (`https://www.linkedin.com/company/<slug>`) and careers URL.
2. **Hunter API Key Support (Optional / Fallback):**
   - Check if the user has a `HUNTER_API_KEY` to provide in `.env.local`.
   - If not provided, use domain heuristics + known tech/fintech company dictionary to populate `sector` and `linkedinUrl`.
3. **Execute Backfill Task:**
   - Run the enrichment with `TRACKER_ALLOW_WRITES=1` against `job-search-fnc314-dev`.
   - Verify that `sector` and `linkedinUrl` populate on Firestore documents.

---

## 3. User Review Required

> [!IMPORTANT]
> **API Key vs Heuristic/Dictionary Enrichment:**
> - Hunter.io requires a paid/free `HUNTER_API_KEY` for `/v2/companies` metadata (industry & LinkedIn).
> - Would you like to provide a `HUNTER_API_KEY` in `.env.local`, OR should we equip the backfill script with a dictionary/heuristic mapper for the 94 existing companies?

---

## 4. Verification Plan

1. Verify Firestore state before run: 94 companies missing `sector` and `linkedinUrl`.
2. Run backfill: `mise run tracker:companies:enrich`.
3. Query Firestore after run to confirm `sector` and `linkedinUrl` counts increase.
