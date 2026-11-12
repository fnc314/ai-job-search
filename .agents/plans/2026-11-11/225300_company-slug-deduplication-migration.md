# Migration Plan: Company Slug Deduplication & Document ID Renaming

**Status:** Completed  
**Date:** 2026-11-11  
**Target:** tracker (cross-subsystem: backend, scripts, Firestore, GCS, local filesystem)  
**Harness:** Junie  
**Model:** Qwen/Qwen3.6-27B-FP8  

---

## 1. Executive Summary & Goals

When a company slug alias changes (e.g., `ford-motor` → `ford`), all artifacts across the system must be migrated atomically so no residues remain. This plan documents the complete migration pipeline and serves as a reusable template for future slug changes.

**Goals:**
- Rename Firestore application document IDs to match the new canonical slug
- Migrate all subcollections (events, artifacts) to the new document
- Update all email references pointing to the old document ID
- Relocate GCS blobs from the old prefix to the new prefix
- Update Firestore artifact documents that reference old GCS paths
- Migrate local `job-applications/` folders to the new slug
- Delete all old artifacts (Firestore documents, GCS blobs, local folders)

## 2. Architecture & Design Principles

### Artifact Locations for a Company Slug

| Layer | Path Pattern | Example (old → new) |
|---|---|---|
| Firestore `applications` | `applications/{docId}` | `ford-motor--staff-android-app` → `ford--staff-android-app` |
| Firestore subcollections | `applications/{docId}/events/`, `applications/{docId}/artifacts/` | Follows document ID |
| Firestore `emails` | `emails/{messageId}` with `applicationId` field | `applicationId` updated |
| Firestore `companies` | `companies/{companyId}` | `ford-motor` → `ford` |
| GCS | `applications/{company-slug}/{role-slug}/{date}/{file}` | `applications/ford-motor/...` → `applications/ford/...` |
| Local filesystem | `job-applications/{company-slug}/{role-slug}/` | `ford-motor/` → `ford/` |

### Canonical Slug Authority

- **Kotlin:** `DocumentIdGenerator.slug()` in `records/parser`
- **Python:** `contract.slug()` in `scripts/contract.py`
- Both must mirror each other exactly

## 3. Proposed Changes & Implementation Steps

### Step 1: Update Slug Aliases (Both Kotlin and Python)

**Files:**
- `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/DocumentIdGenerator.kt`
- `scripts/contract.py`
- `scripts/tracker.py` (domain aliases)
- `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/CompanySectorNormalizer.kt`

**Action:** Update `_COMPANY_SLUG_ALIASES` / `companySlugAliases` maps so the old slug resolves to the new canonical slug.

### Step 2: Run Companies Deduplication (Firestore)

**Command:** `TRACKER_ALLOW_WRITES=1 mise run tracker:companies:deduplicate`

**What it does (BackendService.kt):**
1. Merges duplicate company documents into the canonical `companies/{canonicalId}`
2. Deletes non-canonical company documents
3. Updates `companyId` field on all applications
4. **Renames application document IDs** when the slug prefix has changed:
   - Creates new document at canonical ID
   - Copies all subcollections (events, artifacts)
   - Updates all `emails` documents referencing the old application ID
   - Deletes old document and subcollections

### Step 3: Relocate GCS Blobs (if any exist)

**Script:** `scripts/relocate_gcs_blobs.py`

**Commands:**
```bash
# Preview
python scripts/relocate_gcs_blobs.py --dry-run
# Or target specific slug
python scripts/relocate_gcs_blobs.py --old-slug ford-motor --new-slug ford --dry-run

# Execute
TRACKER_ALLOW_WRITES=1 python scripts/relocate_gcs_blobs.py --old-slug ford-motor --new-slug ford
```

**What it does:**
1. Lists all blobs under `applications/{old-slug}/`
2. Copies each blob to `applications/{new-slug}/` preserving metadata
3. Updates all Firestore artifact documents referencing the old path
4. Deletes the old blob

### Step 4: Migrate Local Folders

**Command:** `TRACKER_ALLOW_WRITES=1 mise run tracker align`

**What it does (alignment.py):**
- Detects role subfolders under old slug and renames them
- Updates `outcome.md` files with canonical slugs

**Manual fallback:**
```bash
mv job-applications/{old-slug}/ job-applications/{new-slug}/
```

### Step 5: Update Tests

**Files to update:**
- `records/parser/src/test/kotlin/dev/fnc314/tools/jobsearch/records/parser/DocumentIdGeneratorTest.kt`
- `records/parser/src/test/kotlin/dev/fnc314/tools/jobsearch/records/parser/CompanySectorNormalizerTest.kt`
- `scripts/tests/test_logo_fetch.py`
- Any other test referencing the old slug

### Step 6: Verification Checklist

- [ ] `mise run tracker:companies:deduplicate --dry-run` shows 0 pending merges and correct rename count
- [ ] `mise run tracker list` shows the new document ID
- [ ] `mise run tracker get {new-doc-id}` returns the migrated document with events intact
- [ ] GCS dry-run shows 0 blobs remaining at old prefix
- [ ] `job-applications/` folder structure uses new slug
- [ ] Kotlin tests pass: `./gradlew :records:records-parser:test`
- [ ] Python tests pass: `python -m pytest scripts/tests/`

## 4. Verification Plan & Testing Checklist

### Ford Migration Results (2026-11-11)

| Artifact | Status | Details |
|---|---|---|
| Firestore `companies/ford` | ✓ Merged | `ford-motor`, `ford-motor-company` deleted |
| Firestore `applications/ford--staff-android-application-developer` | ✓ Renamed | From `ford-motor--staff-android-application-developer` |
| Subcollections (events) | ✓ Migrated | 3 events preserved |
| Email references | ✓ Updated | No emails referenced the old ID |
| GCS blobs | ✓ N/A | No blobs existed at `applications/ford-motor/` |
| Local folder | ✓ Migrated | `job-applications/ford-motor/` → `job-applications/ford/` |
| Kotlin tests | ✓ Pass | `DocumentIdGeneratorTest`, `CompanySectorNormalizerTest` |
| Python tests | ✓ Pass | `test_logo_fetch.py` |

## 5. Bookkeeping

- [x] Plan persisted at `.agents/plans/2026-11-11/225300_company-slug-deduplication-migration.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`
