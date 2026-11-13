---
sessionId: session-261112-210228-ghb5
---

# Requirements

### Overview & Goals

Audit the Firestore `companies` collection to identify and remove companies that:
1. **Have no Hunter.io enrichment data** (no `sector`, `linkedinUrl`, `website`, `description`, or `hunterIoId`)
2. **Have never had an application associated** (metrics.total = 0)

These orphan records are noise in the UI and tracker, and many are garbage entries created by overly aggressive email parsing regex patterns.

### Current State (from live audit)

- **369 total companies** in Firestore (initial count)
- **94 enriched** (have Hunter.io data like sector, linkedinUrl, website)
- **269 companies with no enrichment AND 0 applications** (prime removal candidates)
- **18 confirmed garbage patterns** including:
  - `human-resources-me` — extracted from "human resources me <hr@...>" sender text
  - `careers.dolby.com`, `careers.wipro.com`, `jobs.paccar.com` — URLs parsed as company names
  - `a3c41b8b71eff8c4` — hash-like ID from rogue parsing
  - `360itprofessionals1`, `avanceconsultingservices2` — number-suffix dedup artifacts
  - `epoch-recruiters`, `innovex-recruitment` — recruitment agencies, not employers
  - `major-polish-mobile-telecom-operator` — anonymous job posting
  - `amazon` — real company but 0 applications, no enrichment

### Post-Cleanup State

- **102 total companies** in Firestore (after full audit and migration)
- **0 garbage entries** remaining
- **40 companies** moved to new `potentials` collection (well-known companies with no direct interaction)
- **151 companies** deleted (garbage, consulting firms, obscure names with no interaction)
- **2 Motorola entries** preserved on keep list (user confirmed real applications)
- **100 companies** with applications (unchanged)

### Scope

**In Scope:**
- Build a `companies:audit` CLI command in Kotlin that identifies orphan companies (no enrichment + 0 apps)
- Delete confirmed garbage companies (URLs, hash IDs, recruitment agencies, anonymous postings)
- Harden `extractCompany()` in `EmailClassifier.kt` to prevent future garbage entries
- Add a deny-list of known non-employer patterns to block recruitment agencies and job boards
- All implementation in Kotlin — no new `.py` files

**Out of Scope:**
- Running Hunter.io enrichment on remaining companies (that's `companies:enrich`)
- Enriching the `potentials` collection with career page URLs, LinkedIn, and homepage (follow-up task)
- Modifying Python scripts — all changes go through Kotlin

### User Stories

- As a user, I want the UI to only show companies I've actually applied to or that are real employers, so I don't see noise like "human resources me" or "careers.dolby.com"
- As a maintainer, I want the email parser to reject obvious non-employer patterns so garbage doesn't accumulate again


# Technical Design

### Current Implementation

**Email parsing** (`EmailClassifier.kt::extractCompany`, lines 257-277):
Two regex patterns extract company names from outreach email sender/subject:
```kotlin
"([a-z][a-z ]+?)\\s+(?:recruiting|recruitment|talent|hr|hiring|recruiter)"
"([a-z][a-z ]+?)\\s+(?:careers|jobs|opportunities)"
```
These are too greedy — "human resources me" matches because "human resources" precedes "hr". URLs like "careers.dolby.com" match because "careers" precedes the domain.

**Company creation flow**: Outreach emails → `extractCompany()` → `leadCompany` field in proposal → approved via `proposals approve` → company record created in Firestore.

**Existing dedup** (`BackendService.kt::deduplicateCompanies`): Merges companies with duplicate canonical slugs (e.g., `ford-motor` → `ford`). Does not handle orphan removal.

**Existing enrichment** (`BackendService.kt::batchEnrichCompanies`): Checks `sector`, `linkedinUrl`, `hunterIoId` to determine if a company is "fully enriched".

**CLI entry point** (`Main.kt`): Parses `--deduplicate-companies`, `--enrich-batch`, `--emails-backfill` flags and routes to `BackendService` methods.

### Proposed Changes

#### 1. New `auditOrphanCompanies()` method in `BackendService.kt`

Add a method following the pattern of `deduplicateCompanies()`:
- Query all companies from Firestore via `firestore.collection("companies").get().get()`
- For each company, check enrichment fields (`sector`, `linkedinUrl`, `website`, `description`, `hunterIoId`)
- Read `metrics.counts.total` from each company document
- Classify garbage patterns using the same categories as the live audit:
  - **URL**: name contains `.com`, `.org`, `.net`
  - **Hash ID**: document ID matches `^[0-9a-f]{16,}$`
  - **Number suffix**: name ends with a digit (dedup artifact)
  - **Recruitment agency**: name contains deny-list keywords
  - **HR garbage**: name contains generic HR phrases
  - **Anonymous posting**: name contains "major" and "operator"
  - **Job board artifact**: name contains "career poster" etc.
- Return a structured result map with:
  - `totalCompanies`: count
  - `orphanCompanies`: list of companies with no enrichment + 0 apps
  - `garbageCompanies`: list of classified garbage companies
  - `deletedIds`: list of deleted company IDs (when not dry-run)
  - `flaggedCompanies`: list of legitimate-looking orphans kept for review
- With `dryRun = true`: report only, no deletion
- With `dryRun = false`: delete confirmed garbage companies

#### 2. New `--audit-companies` CLI flag in `Main.kt`

Add argument parsing following the existing pattern:
```kotlin
if (args.contains("--audit-companies")) {
    val dryRun = args.contains("--dry-run")
    println("Executing company audit locally (dryRun: $dryRun)...")
    val result = BackendService.auditOrphanCompanies(dryRun = dryRun)
    println("Company audit complete: $result")
    return
}
```

#### 3. New `companies:audit` mise task

Create `.config/mise/tasks/tracker/companies/audit.sh` following the pattern from `deduplicate.sh`:
- `--dry-run` (default): report only
- `TRACKER_ALLOW_WRITES=1`: delete garbage companies
- Calls `gradlew -p backend run --args="--audit-companies [--dry-run]"`

#### 4. Harden `extractCompany()` in `EmailClassifier.kt`

Add validation before returning an extracted company name:
- **Reject URLs**: if the extracted name contains `.com`, `.org`, `.net`, or looks like a domain
- **Reject known non-employer patterns**: deny-list of words like `recruit`, `recruitment`, `staffing`, `placements`, `career poster`, `talent acquisition`
- **Reject generic HR phrases**: deny-list of `human resources`, `hr department`, `hr team`
- **Reject anonymous postings**: patterns like "major [industry] operator"
- **Minimum word count**: require at least 2 meaningful words (not just "hr" or "jobs")
- **Reject hash-like strings**: if the result matches `^[0-9a-f]{16,}$`

Add a `NON_EMPLOYER_KEYWORDS` set to `EmailClassifier`:
```kotlin
private val NON_EMPLOYER_KEYWORDS = setOf(
    "recruit", "recruitment", "staffing", "placements",
    "career poster", "career guidant", "talent acquisition",
    "human resources", "hr department", "hr team",
)
```

#### 5. Add unit tests in `EmailClassifierTest.kt`

Add tests covering:
- URL rejection: `careers.dolby.com` → `null`
- Recruitment agency rejection: `epoch recruiters` → `null`
- HR phrase rejection: `human resources me` → `null`
- Hash ID rejection: hash-like strings → `null`
- Legitimate company extraction still works: `TechCorp Recruiting` → `Techcorp`

### File Structure

- **Add**: `.config/mise/tasks/tracker/companies/audit.sh` — mise task wrapper
- **Modify**: `backend/service/src/main/kotlin/dev/fnc314/tools/jobtracker/backend/BackendService.kt` — add `auditOrphanCompanies()` method
- **Modify**: `backend/service/src/main/kotlin/dev/fnc314/tools/jobtracker/backend/Main.kt` — add `--audit-companies` flag
- **Modify**: `backend/email-classification/src/main/kotlin/dev/fnc314/tools/jobtracker/classification/EmailClassifier.kt` — harden `extractCompany()` with deny-list
- **Modify**: `backend/email-classification/src/test/kotlin/dev/fnc314/tools/jobtracker/classification/EmailClassifierTest.kt` — add tests for hardened extraction


# Delivery Steps

### ✓ Step 1: Implement companies:audit in BackendService.kt and wire up CLI
Add the `auditOrphanCompanies()` method to `BackendService.kt`, the `--audit-companies` flag to `Main.kt`, and the mise task wrapper.
- Add `auditOrphanCompanies(dryRun: Boolean): Map<String, Any>` to `BackendService.kt` following the pattern of `deduplicateCompanies()`. Query all companies, flag no-enrichment + 0-app records, classify garbage patterns (URL, hash ID, recruitment agency, anonymous posting, number suffix), and return a structured result map.
- Add `--audit-companies` argument parsing to `Main.kt` following the pattern of `--deduplicate-companies`.
- Create `.config/mise/tasks/tracker/companies/audit.sh` mise task wrapper (following `deduplicate.sh`), supporting `--dry-run` (default, report only) and `TRACKER_ALLOW_WRITES=1` (delete garbage companies).
- Run `mise run tracker:companies:audit --dry-run` to generate the initial report and verify classification accuracy.
- Run with `TRACKER_ALLOW_WRITES=1` to delete the 18 confirmed garbage companies (URLs like `careers.dolby.com`, hash IDs like `a3c41b8b71eff8c4`, recruitment agencies like `epoch-recruiters`, HR garbage like `human-resources-me`, anonymous postings like `major-polish-mobile-telecom-operator`).
- Regenerate the Sheet view via `mise run tracker sheet --sync` so the UI reflects the cleanup.

### ✓ Step 2: Harden extractCompany() in EmailClassifier.kt with tests
Add validation to the Kotlin email company extraction to block non-employer patterns.
- Add `NON_EMPLOYER_KEYWORDS` deny-list to `EmailClassifier.kt` covering: recruitment agencies, staffing firms, job board artifacts, generic HR phrases, and anonymous posting patterns.
- Update `extractCompany()` to reject: URLs (contains `.com`/`.org`/`.net`), hash-like strings (`^[0-9a-f]{16,}$`), names matching deny-list keywords, and single-word results shorter than 3 characters.
- Add unit tests to `EmailClassifierTest.kt` covering: URL rejection, recruitment agency rejection, HR phrase rejection, hash ID rejection, and legitimate company extraction still working.
- Run `./gradlew :email-classification:test` to verify tests pass.

### ✓ Step 3: Expand audit to catch broader garbage patterns and delete interactively
The initial audit only caught 17 garbage entries. User reported still seeing nonsense companies in the mobile app UI, so we expanded the garbage classification patterns and walked through each entry interactively.
- Expanded `auditOrphanCompanies()` in `BackendService.kt` to catch: corporate suffixes (KG, GmbH, LTD, AB, Co., BV, LLP, PVT), consulting firms, search firms, agencies, creative agencies, professional services, and generic industry suffixes (group, technologies, services, solutions, systems, global, international).
- Added `keepList` exception for `motorola-solutions` (user confirmed real applications from summer emails).
- Walked through 58 garbage candidates in 6 batches with user approval via y/n decisions.
- Deleted **55 garbage companies** from Firestore (consulting firms, corporate suffixes, group/tech/services/solutions suffixes, search firms, agencies, etc.).
- Post-cleanup audit: 293 total companies, 0 garbage, 193 flagged orphans (legitimate-looking names like Amazon, IBM, Cisco kept for manual review).

### ✓ Step 4: Full audit — companies database should only hold entities with direct interaction
User clarified that the companies database should only hold entities with direct interaction (via /scrape, /apply, or external tracker addition). No "potential prospects" or entries "just because they're real companies".
- Added `migrateZeroAppToPotentials()` to `BackendService.kt` — queries all companies, identifies zero-app entries, classifies garbage for deletion, moves well-known companies to a new `potentials` collection, and keeps Motorola entries.
- Added `--migrate-potentials` CLI flag to `Main.kt`.
- Created `.config/mise/tasks/tracker/companies/migrate-potentials.sh` mise task wrapper.
- Walked through 193 zero-app companies in categories with user approval.
- **Deleted 151 companies** (garbage, consulting firms, obscure names with no interaction).
- **Moved 40 companies** to new `potentials` collection (well-known companies like Amazon, IBM, Stripe, Visa, etc.).
- **Kept 2 Motorola entries** (user confirmed real applications).
- Final state: 102 companies in Firestore (100 with apps + 2 Motorola), 40 in potentials, 0 garbage.
- Follow-up: Enrich `potentials` collection with career page URLs, LinkedIn, and homepage.