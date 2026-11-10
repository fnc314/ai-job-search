# Implementation Plan: RM-005 LinkedIn Company Enrichment

**Status:** Proposed  
**Date:** 2026-11-10  
**Target:** `scripts/scrape.py`, `backend/`, `.agents/skills/linkedin-search/`, `companies` Firestore collection  
**Evaluates:** `RM-005` in [ROADMAP.md](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ROADMAP.md)  
**Schema Authority:** `tracker.schema.json` v10 (`$defs/company`)

---

## Goal Description
Implement **RM-005 (LinkedIn Company Enrichment)** from [`ROADMAP.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ROADMAP.md).
When job postings are discovered or scraped during the `/scrape` workflow (specifically `scripts/scrape.py`), new or updated postings identify company entities. RM-005 requires enriching the `companies` Firestore collection with company metadata—specifically `sector`/industry tags, LinkedIn profile URL, and logo metadata—so that this enriched data is readily available when roles are evaluated and surfaced to `/apply`.

Enrichment must be non-blocking and resilient to network/rate-limit failures, respecting LinkedIn usage boundaries and caching company lookups by canonical company slug (`companyId`).

---

## User Review Required
> [!IMPORTANT]
> **Deterministic Backend Service vs Script Integration:**
> Per `RM-003` (Backend Functionality Abstraction), deterministic operations should ideally be exposed via `backend/` services.
> We can expose a `POST /companies/enrich` endpoint in `backend/` that accepts a company slug/name, checks existing Firestore `companies/{companyId}`, and enriches missing `sector`, `linkedinUrl`, and `logoUrl`. `scripts/scrape.py` calls this endpoint (or falls back locally if offline/standalone).
> 
> **Rate Limiting & Safety:**
> Per LinkedIn ToS, company detail scraping must be bounded (e.g. at most 1 query per distinct unseen company per scrape run, caching results in Firestore).

---

## Open Questions
- Should `companies/{companyId}.sector` map to standard sector labels (e.g., `FinTech`, `Software`, `Healthcare`) or preserve the raw LinkedIn industry string?
  *(Recommendation: Normalize known sectors like "Financial Services" -> "FinTech", "Computer Software" -> "Technology", and preserve the raw string in `metadata.linkedinIndustry`)*.

---

## Proposed Changes

### 1. `backend/` (Kotlin Cloud Run Service)
#### [MODIFY] `BackendService.kt`
- Implement `enrichCompany(companyId: String, companyName: String, linkedinUrl: String? = null): Map<String, Any?>`:
  - Check `companies/{companyId}` in Firestore.
  - If `sector` or `logoUrl` or `linkedinUrl` is already populated and fresh, skip fetching.
  - If missing, leverage existing Hunter.io / Clearbit / LinkedIn guest metadata or logo fetcher.
  - Write non-destructively via `SetOptions.merge()`.

#### [MODIFY] `Main.kt`
- Add route `POST /companies/enrich`:
  - Accepts JSON: `{"companyId": "...", "name": "...", "linkedinUrl": "..."}`.
  - Returns enriched company record.

---

### 2. `scripts/` & `.agents/skills/`
#### [MODIFY] `scripts/scrape.py`
- In `save_scraped_postings()` or during post-processing of candidate postings:
  - Collect distinct companies among new or top-fit postings.
  - Invoke company enrichment (via backend service if available, or local non-blocking fetcher).
  - Ensure any network timeout or scrape error is caught and logged without aborting the main job scraping.

---

### 3. `ROADMAP.md`
#### [MODIFY] `ROADMAP.md`
- Update `RM-005` status from `OPEN` to `IN_PROGRESS` (and subsequently `DONE` upon verification).
- Add linked plan `.agents/plans/2026-11-10/152500_linkedin_company_enrichment_plan.md`.

---

## Verification Plan

### Automated Tests
1. **Records & Backend Tests:**
   ```bash
   ./gradlew -p backend test
   ```
2. **Schema & Tracker Alignment:**
   ```bash
   mise run tracker:check
   python3 -m unittest scripts/tests/test_tracker_schema.py
   ```

### Manual Verification
1. Run a test scrape or run enrichment on a specific company (e.g. `capital-one`):
   ```bash
   python3 scripts/scrape.py --query "Android" --limit 3 --no-write
   ```
2. Inspect Firestore `companies` collection to verify `sector`, `logoUrl`, and `metadata` are properly persisted according to `tracker.schema.json`.
