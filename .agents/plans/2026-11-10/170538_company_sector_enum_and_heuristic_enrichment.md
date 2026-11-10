# Implementation Plan: CompanySector Enum & Refined Heuristic Company Enrichment (RM-005)

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `records-proto`, `records-parser`, `backend/`, `frontend/`, Firestore `companies` (`job-search-fnc314-dev`)  
**Evaluates:** User feedback on `heuristic_company_enrichment_backfill.md` (L26): "Let's define an `enumeration` for these sectors. This list can grow as companies are parsed, but FINTECH, E_COMMERCE, AUTOMOTIVE, EDUCATION, CONSULTING, TECH, etc... are all valid and should be constants"  
**Schema Authority:** `records/proto/src/main/proto/tracker/v1/enums.proto`, `tracker.schema.json`  

---

## 1. Goal Description

Define a canonical, strongly-typed `CompanySector` enumeration in `records-proto` and Kotlin/Multiplatform layers, establish exact mappings for all known and parsed sectors, and execute the company enrichment backfill across all 94 Firestore companies with standardized sector constants and verified LinkedIn URLs.

---

## 2. Technical Design

### 2.1. Protobuf & Domain Enum Definition: `CompanySector`

Add `CompanySector` to `records/proto/src/main/proto/tracker/v1/enums.proto`:
```protobuf
// ============================================================================
// CompanySector
// Normalized business domain or market sector classification
// ============================================================================
enum CompanySector {
  COMPANY_SECTOR_UNSPECIFIED = 0;
  COMPANY_SECTOR_TECH = 1;
  COMPANY_SECTOR_FINTECH = 2;
  COMPANY_SECTOR_HEALTHCARE = 3;
  COMPANY_SECTOR_E_COMMERCE = 4;
  COMPANY_SECTOR_AUTOMOTIVE = 5;
  COMPANY_SECTOR_MEDIA = 6;
  COMPANY_SECTOR_EDUCATION = 7;
  COMPANY_SECTOR_CONSULTING = 8;
  COMPANY_SECTOR_AEROSPACE = 9;
  COMPANY_SECTOR_RETAIL = 10;
  COMPANY_SECTOR_ENERGY = 11;
  COMPANY_SECTOR_TELECOM = 12;
}
```

Expose human-friendly wire strings and normalization helpers in `records/parser` (e.g. `CompanySectorParser` / `StatusNormalizer` style):
- Wire / Storage format: standard canonical string `FINTECH`, `TECH`, `HEALTHCARE`, `E_COMMERCE`, `AUTOMOTIVE`, `MEDIA`, `EDUCATION`, `CONSULTING`, `RETAIL` (or lowercase kebab / title if specified; default matches existing schema standard `FINTECH` / `FinTech` with display resolver).

### 2.2. Sector Mapping & Dictionary

Define `CompanySector` mapping table for existing 94 companies:
- **FINTECH**: `affirm`, `bankjoy`, `capital-one`, `coinbase`, `edward-jones`, `fidelity-investments`, `huntington`, `jpmorgan-chase`, `pnc`, `polymarket`, `ramp`, `robinhood`, `spoton`, `unit21`, `vanguard`
- **TECH**: `anthropic`, `bitwarden`, `bigcommerce`, `block`, `canary-technologies`, `crestron-electronics`, `dropbox`, `duolingo`, `fetch`, `github`, `google`, `hireeazy`, `meta`, `microsoft`, `netflix`, `nextdoor`, `openai`, `revenuecat`, `salesforce`, `seatgeek`, `sentilink`, `slack`, `snap`, `speechify`, `talentry`, `teamsnap`, `textnow`, `toast`, `touchlab`, `trade-desk`, `unity`, `waymo`, `yinzcam`, `zillow`, `ziprecruiter`
- **HEALTHCARE**: `baxter-international`, `boston-scientific`, `butterfly-network`, `centene`, `grow-therapy`, `hims-hers`, `honor`, `karoo-health`, `osmind`
- **E_COMMERCE** / **RETAIL**: `american-eagle-outfitters`, `ebay`, `fanatics`, `grove-collaborative`, `iherb`, `instacart`, `sharkninja`, `slickdeals`, `ulta-beauty`
- **AUTOMOTIVE**: `car-next-door`, `doordash`, `ford-motor-company`, `general-motors`, `lime`, `scout-motors`, `uber`
- **MEDIA**: `disney`, `major-league-baseball`, `medium`, `npr`, `paramount`, `playlist`, `reddit`, `roku`, `washington-post`
- **EDUCATION**: `carnegie-mellon-university`
- **CONSULTING**: `deloitte`, `thomson-reuters`

### 2.3. Heuristic Resolution for Unmapped / Scraped Companies
If a company is not in the predefined map, resolve its `CompanySector` using:
1. Public LinkedIn guest scraper industry string (e.g. `"Financial Services"` -> `FINTECH`, `"Research Services"` -> `TECH`, `"Medical Equipment Manufacturing"` -> `HEALTHCARE`).
2. Domain / Name token matching:
   - "bank", "pay", "invest", "capital", "crypto", "trading" -> `FINTECH`
   - "health", "care", "medical", "clinic", "pharma" -> `HEALTHCARE`
   - "shop", "store", "commerce", "fashion" -> `E_COMMERCE`
   - "auto", "motor", "drive", "transit" -> `AUTOMOTIVE`
   - Default: `TECH`

### 2.4. LinkedIn URL Derivation
- Match official company slug: `https://www.linkedin.com/company/<slug>` (e.g. `https://www.linkedin.com/company/affirm`, `https://www.linkedin.com/company/anthropicresearch`).

---

## 3. Proposed Code Changes

### [MODIFY] `records/proto/src/main/proto/tracker/v1/enums.proto`
Add `CompanySector` enum definition.

### [NEW] `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/CompanySectorNormalizer.kt`
Add bidirectional normalizer between `CompanySector` enum, raw strings (e.g. `"Financial Services"`), and display labels.

### [NEW] `records/parser/src/test/kotlin/dev/fnc314/tools/jobsearch/records/parser/CompanySectorNormalizerTest.kt`
Unit test suite verifying all mappings and token heuristics.

### [MODIFY] `backend/src/main/kotlin/dev/fnc314/tools/jobtracker/backend/BackendService.kt`
Update `enrichCompany()` and `batchEnrichCompanies()` to use `CompanySectorNormalizer`, store the standard `CompanySector.name` in `sector`, and store the raw parsed industry string in `metadata.linkedinIndustry`.

### [MODIFY] `frontend/features/companies-list/data/...` and `frontend/features/company-details/data/...`
Ensure UI models read `CompanySector` or string safely with display fallback.

---

## 4. Verification Plan

### Automated Checks
```bash
./gradlew generateProto -p records
./gradlew test -p records
./gradlew test -p backend
./gradlew detekt -p records -p backend
```

### Database Backfill Execution
```bash
TRACKER_ALLOW_WRITES=1 TRACKER_PROJECT=job-search-fnc314-dev mise run tracker:companies:enrich
```

### Verification Query
Verify via Python/Firestore script:
- All 94 companies in `job-search-fnc314-dev` have valid `sector` matching `CompanySector` names (`FINTECH`, `TECH`, `HEALTHCARE`, etc.).
- All 94 companies have `linkedinUrl` populated.
