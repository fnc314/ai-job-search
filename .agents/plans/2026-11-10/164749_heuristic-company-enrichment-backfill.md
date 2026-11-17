# Implementation Plan: Heuristic & Dictionary Company Enrichment Backfill (RM-005)

**Status:** Proposed  
**Date:** 2026-11-10  
**Target:** `backend/` (`dev.fnc314.tools.jobtracker.backend.BackendService`), `scripts/tracker.py`, Firestore `companies` collection (`job-search-fnc314-dev`)  
**Evaluates:** Option 2 of RM-005 Company Enrichment backfill: Built-in dictionary and heuristic enricher without requiring paid API keys.  
**Schema Authority:** `tracker.schema.json` v10 (`$defs/company`)  

---

## 1. Goal Description

Out of 94 companies in Firestore (`job-search-fnc314-dev`), 0 have `sector` or `linkedinUrl`.
Because `HUNTER_API_KEY` is not present, the existing Hunter.io API integration skipped all 94 companies.
The goal of this plan is to equip `BackendService.kt` with a built-in industry/sector dictionary for all known companies in the database, supplemented by domain-based and slug-based heuristic rules, and LinkedIn URL resolution (`https://www.linkedin.com/company/<slug>`).
We will then execute the batch backfill to populate `sector`, `linkedinUrl`, and `metadata.linkedinIndustry` across all 94 company records in Firestore.

---

## 2. Technical Design

### 2.1. Sector & LinkedIn Dictionary in `BackendService.kt`

Add a static dictionary mapping company slugs and names to standard sectors and official LinkedIn slugs:
- **Sectors:**
  - `FinTech` / `Financial Services`: `affirm`, `bankjoy`, `capital-one`, `coinbase`, `edward-jones`, `fidelity-investments`, `huntington`, `jpmorgan-chase`, `pnc`, `polymarket`, `ramp`, `robinhood`, `spoton`, `unit21`, `vanguard`
  - `Technology` / `AI` / `Software`: `anthropic`, `bitwarden`, `bigcommerce`, `block`, `canary-technologies`, `crestron-electronics`, `dropbox`, `duolingo`, `fetch`, `github`, `google`, `hireeazy`, `meta`, `microsoft`, `netflix`, `nextdoor`, `openai`, `revenuecat`, `salesforce`, `seatgeek`, `sentilink`, `slack`, `snap`, `speechify`, `talentry`, `teamsnap`, `textnow`, `toast`, `touchlab`, `trade-desk`, `unity`, `waymo`, `yinzcam`, `zillow`, `ziprecruiter`
  - `Healthcare` / `HealthTech` / `Biotech`: `baxter-international`, `boston-scientific`, `butterfly-network`, `centene`, `grow-therapy`, `hims-hers`, `honor`, `karoo-health`, `osmind`
  - `E-Commerce` / `Retail`: `american-eagle-outfitters`, `ebay`, `fanatics`, `grove-collaborative`, `iherb`, `instacart`, `sharkninja`, `slickdeals`, `ulta-beauty`
  - `Automotive` / `Transportation`: `car-next-door`, `doordash`, `ford-motor-company`, `general-motors`, `lime`, `scout-motors`, `uber`
  - `Media` / `Entertainment`: `disney`, `major-league-baseball`, `medium`, `npr`, `paramount`, `playlist`, `reddit`, `roku`, `washington-post`
  - `Education`: `carnegie-mellon-university`
  - `Consulting` / `Professional Services`: `deloitte`, `thomson-reuters`

### 2.2. Heuristic Fallback
For any company not in the static map:
1. Examine `website` domain and keywords in `name`:
   - Contains "bank", "pay", "invest", "capital", "financial" -> `FinTech`
   - Contains "health", "medical", "bio", "therapeutics", "care" -> `Healthcare`
   - Contains "retail", "store", "commerce", "shop", "apparel" -> `Retail`
   - Contains "motor", "auto", "mobility" -> `Automotive`
   - Default -> `Technology`
2. LinkedIn URL:
   - Match official company slug: `https://www.linkedin.com/company/<slug>` (or mapped slug).

### 2.3. Batch Enrichment Execution
Update `batchEnrichCompanies()` in `BackendService.kt`:
- Run through each company in `companies`.
- If missing `sector` or `linkedinUrl`, consult the dictionary / heuristics.
- Call `enrichCompany(...)` to non-destructively merge the values and set `lastEnrichedAt`.
- Execute via `mise run tracker:companies:enrich` (or directly via backend CLI).

---

## 3. Proposed Code Changes

### [MODIFY] `backend/src/main/kotlin/dev/fnc314/tools/jobtracker/backend/BackendService.kt`
- Add `COMPANY_SECTORS_MAP: Map<String, Pair<String, String>>` (slug to `Pair(sector, linkedinSlug)`).
- Update `batchEnrichCompanies()`:
  - Check dictionary first.
  - Fall back to heuristic classification if `fetchHunterCompanyInfo` returns empty.
  - Enrich and save.

---

## 4. Verification Plan

### Automated Checks
```bash
./gradlew test -p backend
./gradlew detekt -p backend
```

### Execution & Live Database Verification
```bash
mise run tracker:companies:enrich
```
Run Firestore verification query:
- Verify that `with_sector` increases from 0 to 94.
- Verify that `with_linkedin` increases from 0 to 94.
- Verify that sample companies (`affirm`, `anthropic`, `pnc`, `baxter-international`) have correct `sector` and valid `linkedinUrl`.
