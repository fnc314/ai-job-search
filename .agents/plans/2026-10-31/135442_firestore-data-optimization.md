---
sessionId: session-261031-122009-16pg
---

# Requirements

## Overview & Goals
Replace Google Drive artifact storage with Google Cloud Storage (GCS), and normalize company data into a dedicated `companies` collection so constant company attributes aren't duplicated across application documents.

## Scope

### In Scope
1. **GCS artifact upload**: During `/apply` and `tracker.py artifact`, upload CVs, cover letters, and outcome.md to the existing GCS bucket (`job-search-fnc314-dev-artifacts`). Files are stored at `applications/{company}/{role}/{YYYY-MM-DD}/{filename}`. The artifact document records `storage` (GCS bucket+path) and a signed URL for access.
2. **Remove Google Drive dependency**: Delete `scripts/drive.py`, `scripts/migrate_drive.py`, and `scripts/merge_drive_duplicates.py`. Remove all Drive-related code from `tracker.py`, `tracker_tui.py`, and the schema.
3. **GCS migration script**: A one-shot script that uploads all existing local artifacts from `documents/applications/` to GCS and backfills the `storage` field on existing artifact documents in Firestore.
4. **Companies collection normalization**: Pull constant company attributes into a top-level `companies` collection. Applications reference companies via `companyId` (already present).

### Out of Scope
- Modifying the Google Sheet view or HTML report
- Two-tier data loading or session caching (those remain future considerations)
- The Google Drive folder — existing files there can be manually cleaned up; no code will interact with Drive anymore

## Key Decisions

### GCS, not Google Drive
Google Drive proved unsuitable for programmatic artifact storage: the sync client creates duplicate folders (`(1)` suffixes), the API and local filesystem are out of sync, and service accounts have 0 storage quota requiring OAuth. GCS is proper object storage with:
- **Deterministic paths** — no folder duplication issues
- **Reliable existence checks** — `blob.exists()` is authoritative
- **Service account access** — no OAuth dance needed
- **Signed URLs** — shareable, time-limited access links
- **Existing infrastructure** — `scripts/artifacts.py` and `client.getBucket()` already handle GCS uploads

### GCS path structure matches Drive structure
Files are stored at `applications/{company}/{role}/{YYYY-MM-DD}/{filename}` in the bucket, matching the previous Drive structure. This keeps the path human-readable and consistent with the local `documents/applications/` directory.

### Company creation is automatic
When `tracker.py add` or `upsert` writes an application, it checks the `companies` collection for the `companyId`. If missing, it creates a company document with whatever company fields are available in the payload. No separate command is needed for company creation.

### Detail panel reads from Firestore, not filesystem
The TUI detail panel replaces `get_archive_dir()` and `load_detail_lines()` filesystem lookups with Firestore reads: artifact `storage` (GCS path) values from the application document, and company profile from the `companies` collection.

# Technical Design

## Current State

### Artifact Storage Today
- **`scripts/artifacts.py`**: Uploads to GCS (`gs://job-search-fnc314-dev-artifacts`) via `google-cloud-storage`. Artifact documents record `storage` (GCS bucket+path), `localPath`, `driveUrl`, or `externalUrl`. The module already has `upload()`, `ensureUpload()`, `exists()`, and `remoteHash()` functions.
- **`scripts/client.py`**: `getBucket()` returns the GCS bucket handle. `ARTIFACT_BUCKET = "job-search-fnc314-dev-artifacts"`.
- **`scripts/drive.py`**: Google Drive API wrapper (user OAuth, not service account). Creates duplicate folders on re-runs due to sync/API mismatch. **Will be deleted.**
- **`scripts/migrate_drive.py`**: Migration script that uploads to Drive. Creates duplicates. **Will be replaced with GCS migration.**
- **`scripts/merge_drive_duplicates.py`**: Cleanup script for Drive duplicates. **Will be deleted.**
- **`scripts/tracker.py`**: `commandArtifact()` uploads to GCS and optionally Drive. `buildArtifacts()` resolves `localPath`/`driveUrl`/`externalUrl` from payload. Drive upload calls go through `drive.upload_artifact()`.
- **`ui/terminal/tracker_tui.py`**: `load_detail_lines()` shows `[DRIVE]` links for artifacts with `driveUrl`. For `.md` files, downloads from Drive via `drive.download_file()` for inline rendering. Non-`.md` files open in browser.
- **Problem**: Drive-based artifact storage creates endless duplicates. The sync client and API are out of sync, making reliable existence checks impossible.

### Company Data Today
- **`tracker.schema.json`**: Already defines a `company` type with `companyId`, `name`, `aliases`, `sector`, `careersUrl`, `notes`, `metadata`.
- **`scripts/client.py`**: Already defines `COMPANIES = "companies"` as a collection name.
- **`scripts/tracker.py`**: Applications have `companyId` (derived kebab slug from company name) but the `companies` collection is never written to or read from.
- **Problem**: Company attributes (name, sector, etc.) are duplicated on every application document. There's no single source of truth for company data.


## Proposed Changes

### 1. Remove Google Drive dependency

Delete all Drive-related code and files:

- **Delete `scripts/drive.py`**: Remove the entire Drive API wrapper module.
- **Delete `scripts/migrate_drive.py`**: Replace with GCS migration script.
- **Delete `scripts/merge_drive_duplicates.py`**: No longer needed — GCS has no duplicate issue.
- **`scripts/tracker.py`**: Remove all `import drive` and `driveModule.upload_artifact()` calls. The GCS upload path in `commandArtifact()` already works via `artifacts.upload()`.
- **`ui/terminal/tracker_tui.py`**: Replace `[DRIVE]` file display with `[GCS]` using signed URLs. For `.md` files, download from GCS for inline rendering via `blob.download_as_text()`.
- **`tracker.schema.json`**: Remove `driveUrl` and `driveFileId` from the `artifact` type. The `storage` field (GCS bucket+path) is the authoritative location.
- **`scripts/artifacts.py`**: Add `signed_url(bucket, name, expiration_hours=7)` helper that returns a time-limited download URL. Add `download_text(bucket, name)` for inline `.md` rendering.
- **`requirements-tracker.txt`**: Remove `google-auth-oauthlib` (no longer needed for Drive OAuth).

### 2. GCS Migration Script (`scripts/migrate_gcs.py`)

A one-shot script that uploads all local artifacts to GCS and backfills Firestore:

- Scans `documents/applications/` for existing artifact files (CVs, cover letters, outcome.md, etc.).
- Uploads to GCS at `applications/{company}/{role}/{YYYY-MM-DD}/{filename}` using `contract.normalize_dir_name()` for company/role names.
- Uses `artifacts.ensureUpload()` for idempotent uploads (sha256 dedup).
- Finds the corresponding artifact document in Firestore (by matching company, role, filename).
- Backfills the `storage` field on the artifact document.
- Pre-builds a Firestore artifact index for O(1) lookups.
- Progress counter (`[N/total]`).
- `--dry-run` shows tree view of GCS structure that will be created.
- `--company <name>` filter for testing.

### 3. Update `scripts/tracker.py` Write Path

**`buildArtifacts()`**: When an artifact has `localPath`, upload to GCS via `artifacts.ensureUpload()`. The company, role, and date are extracted from the application document context. On success, set `storage` to the GCS bucket+path. Keep `localPath` as a fallback. On failure, fall back to `localPath` only — don't fail the write.

**`commandArtifact()`**: Already uploads to GCS via `artifacts.upload()`. Remove the Drive upload path. Record `storage` (GCS) only.

**`commandAdd()` / `commandUpsert()`**: After writing the application, ensure the company document exists in the `companies` collection. Extract company fields from the payload:
- `companyId`: derived slug
- `name`: company name
- `sector`: from payload
- `website`: from payload (new optional field)
- `hqAddress`: from payload (new optional field)
- `careersEmail`: from payload (new optional field)
- `jobBoardUrl`: from payload (new optional field, e.g. Greenhouse/Lever base URL)
- `linkedinUrl`: from payload (new optional field)

### 4. Update `tracker.schema.json`

**`company` type**: Add fields:
- `website`: nullableString
- `hqAddress`: nullableString
- `careersEmail`: nullableString
- `jobBoardUrl`: nullableString
- `linkedinUrl`: nullableString

**`artifact` type**: Remove `driveUrl` and `driveFileId`. Keep `storage` (GCS bucket+path) as the primary location. Keep `localPath` as offline fallback and `externalUrl` for third-party links.

### 5. Update `scripts/reader.py` Read Path

**`rowsFromStore()`**: After fetching applications, batch-fetch company documents for all distinct `companyId` values. Merge company data into each row as `_company` (prefixed to avoid colliding with application fields).

**`badge()`**: Include company source in the metadata label.

### 6. Update `ui/terminal/tracker_tui.py` Detail Panel

**`load_detail_lines()`**: Replace filesystem lookups:
- **Files section**: Instead of `get_archive_dir()` scanning `documents/applications/` on the local filesystem, read artifact `storage` (GCS path) values from the row. Show GCS signed URLs as `[GCS]` links. Also show local `localPath` if the file exists locally (for offline access).
- **Company section**: Read `_company` data merged by `reader.py` and display company profile (name, website, sector, HQ, careers email, job board, LinkedIn).
- **Outreach protocols**: Keep the local JSON file lookup (this is a static config file, not user data).

**File view**: When the user presses Enter on a GCS artifact for a `.md` file, download from GCS via `blob.download_as_text()` and render inline. For non-`.md` files, generate a signed URL and open in browser.

### 7. Update `scripts/cache.py`

**`readAll()`**: Fetch companies collection and include in cache. Build a company index alongside applications.

## Architecture Diagram

``` mermaid
graph LR
  A[tracker.py add/upsert] -->|1. buildArtifacts| B[artifacts.py ensureUpload]
  B -->|2. GCS API| C[GCS bucket applications/]
  A -->|3. ensure company| D[companies collection]
  A -->|4. write application| E[applications collection]
  E -.->|companyId ref| D
  F[reader.py rowsFromStore] -->|5. batch fetch| E
  F -->|6. batch fetch| D
  F -->|7. merge| G[rows with _company data]
  G --> H[tracker_tui.py detail panel]
  H -->|8. GCS signed URL| I[browser or inline .md]
```

## File Structure

| File | Change |
|---|---|
| `scripts/drive.py` | **Deleted** — Drive API wrapper, no longer needed |
| `scripts/migrate_drive.py` | **Deleted** — replaced by `scripts/migrate_gcs.py` |
| `scripts/merge_drive_duplicates.py` | **Deleted** — GCS has no duplicate issue |
| `scripts/migrate_gcs.py` | **New** — One-shot migration script for existing local artifacts to GCS |
| `scripts/artifacts.py` | Modified — add `signed_url()` and `download_text()` helpers |
| `scripts/tracker.py` | Modified — remove Drive upload, keep GCS upload; add company creation |
| `scripts/reader.py` | Modified — batch-fetch companies, merge into rows |
| `scripts/cache.py` | Modified — include companies in cache |
| `tracker.schema.json` | Modified — add company fields, remove driveUrl/driveFileId from artifact |
| `ui/terminal/tracker_tui.py` | Modified — detail panel reads from Firestore/GCS instead of filesystem/Drive |
| `requirements-tracker.txt` | Remove `google-auth-oauthlib` (no longer needed) |

# Delivery Steps

### ✓ Step 1: Remove Google Drive dependency and add GCS helpers
All Drive-related code is removed; GCS becomes the sole artifact storage backend.

- Delete `scripts/drive.py`, `scripts/migrate_drive.py`, and `scripts/merge_drive_duplicates.py`
- Add `signed_url(bucket, name, expiration_hours=7)` to `scripts/artifacts.py` — returns a time-limited GCS download URL
- Add `download_text(bucket, name)` to `scripts/artifacts.py` — downloads blob content as text for inline `.md` rendering
- Remove all `import drive` / `driveModule.upload_artifact()` calls from `scripts/tracker.py`
- Remove `driveUrl` and `driveFileId` from `tracker.schema.json` artifact type
- Remove `google-auth-oauthlib` from `requirements-tracker.txt`
- Update `ui/terminal/tracker_tui.py`: replace `[DRIVE]` display with `[GCS]` using signed URLs; replace `drive.download_file()` with `artifacts.download_text()` for `.md` inline rendering

### ✓ Step 2: GCS migration script for existing local artifacts
All ~310 local artifacts in `documents/applications/` are uploaded to GCS and Firestore is backfilled.

- Create `scripts/migrate_gcs.py`:
  - Scans `documents/applications/` for existing artifact files
  - Uploads to GCS at `applications/{company}/{role}/{YYYY-MM-DD}/{filename}` using `contract.normalize_dir_name()` for path names
  - Uses `artifacts.ensureUpload()` for idempotent uploads (sha256 dedup)
  - Finds corresponding artifact document in Firestore and backfills `storage` field
  - Pre-builds Firestore artifact index for O(1) lookups
  - Progress counter (`[N/total]`), `--dry-run` tree view, `--company` filter
- Run migration: `TRACKER_ALLOW_WRITES=1 .venv/bin/python scripts/migrate_gcs.py`
- Verify: all artifacts have `storage` field in Firestore, no errors

### ✓ Step 3: Update tracker.py write path and populate companies collection
New artifact uploads go to GCS only; company documents are created on application write.

- Update `scripts/tracker.py` `buildArtifacts()`: upload to GCS via `artifacts.ensureUpload()` instead of Drive. Set `storage` on the artifact document.
- Update `scripts/tracker.py` `commandArtifact()`: remove Drive upload path, keep GCS upload via `artifacts.upload()`.
- Add `ensure_company(db, company_doc)` in `scripts/tracker.py`: upserts company document in `companies` collection
- Update `commandAdd()` and `commandUpsert()`: call `ensure_company()` after writing the application
- Update `tracker.schema.json` `company` type: add `website`, `hqAddress`, `careersEmail`, `jobBoardUrl`, `linkedinUrl`

### ✓ Step 5: Align GCS path structure and naming conventions
All GCS uploads use the canonical path `applications/{company}/{role}/{YYYY-MM-DD}/{filename}` with normalized names.

- **Problem**: `artifacts.py`'s `blobPath()` uses a flat 2-level structure `applications/{id}/{kind}/{filename}` without role or date. `tracker.py`'s `buildArtifacts()` passes `company.lower().replace(" ", "-")` instead of using `contract.normalize_dir_name()`. This means new uploads via `/apply` or `tracker.py artifact` will create paths inconsistent with the migration script's 4-level normalized paths.
- **`scripts/artifacts.py`**: Update `blobPath()` to accept `company`, `role`, and `date` parameters and use `contract.normalize_dir_name()` for company/role. The new signature: `blobPath(company, role, date, kind, filename)` → `applications/{norm_company}/{norm_role}/{date}/{kind_leaf}/{filename}`. Keep backward compatibility via `upload()` and `ensureUpload()` by having them construct the full path.
- **`scripts/tracker.py`**: Update `buildArtifacts()` to pass `company`, `role`, and `date` to `artifacts.ensureUpload()` using normalized names. Update `commandArtifact()` similarly.
- **`scripts/migrate_gcs.py`**: Already correct — uses `build_gcs_path()` with `normalize_dir_name()`. No changes needed.
- Verify: `migrate_gcs.py --dry-run` shows consistent paths with what `tracker.py` would produce.

### ✓ Step 4: Enrich reads with company data and update TUI detail panel
The detail panel shows company profile from Firestore and GCS artifact links instead of local file paths.

- Update `scripts/reader.py` `rowsFromStore()`: batch-fetch company documents for all distinct `companyId` values; merge into each row as `_company`
- Update `scripts/cache.py` `readAll()`: fetch companies collection and include in cache
- Update `ui/terminal/tracker_tui.py` `load_detail_lines()`:
  - Replace `get_archive_dir()` filesystem scan with artifact `storage` (GCS) values from the row
  - Show GCS signed URLs as `[GCS]` links
  - Show local `localPath` files if they exist (offline fallback)
  - Add company profile section from `_company` data (name, website, sector, HQ, careers email, job board, LinkedIn)
  - Keep outreach protocols from local JSON (static config)
- Detail panel is machine-independent and shows rich company context