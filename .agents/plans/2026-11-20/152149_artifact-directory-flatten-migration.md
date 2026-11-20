# Migration Plan: Flatten Artifact Directory Structure (Date-First)

**Status:** Draft / Proposed  
**Date:** 2026-11-20  
**Target:** repository-wide  
**Harness:** Antigravity  
**Model:** claude-4-5-sonnet  
**Session:** session-261120-144117-ig0t  
**Depends On:** [`135442_firestore-data-optimization.md`](../../.agents/plans/2026-10-31/135442_firestore-data-optimization.md)  

---

## 1. Executive Summary & Goals

Flatten the `job-applications/` directory structure from `COMPANY/ROLE/YYYY-MM-DD/*` to `COMPANY/ROLE/YYYY-MM-DD_*` by removing the date subdirectory and prefixing each filename with its date. This eliminates a redundant nesting level (96% of roles have only one date folder), consolidates outcome updates in a single directory, and simplifies browsing and scripting.

**Goals:**
1. Reduce directory depth from 4 levels to 3 levels for all artifacts
2. Preserve chronological ordering via date-first filename prefix
3. Maintain GCS path consistency with local filesystem
4. Migrate all existing local files and resync GCS
5. Update all code and documentation surfaces referencing the old pattern

## 2. Architecture & Design Principles

### Current Structure

**Local filesystem** (`job-applications/`):
- 128 company directories, ~974 files (excluding build artifacts, `.DS_Store`, fonts)
- 96% of files at depth 4: `company/role/date/file`
- 32 files at depth 5 (nested role folders, `OpenFonts/` subdirs)
- 1 file at depth 6

**GCS** (`gs://bucket/applications/`):
- Same 4-level structure: `applications/{company}/{role}/{date}/{filename}`
- Built by `ArtifactUtils.blobPath()` in `records/parser/src/main/kotlin/.../ArtifactMetadata.kt`

### Key Finding: The Date Folder is Mostly Redundant

- 96 roles have a single date folder
- 77 roles have multiple date folders, but the additional dates almost always contain only `outcome.md` updates — not distinct artifacts
- Example: `capital-one/android-mobile-engineer-4/` has 3 dates: `2026-08-25/` (CV PDFs), `2026-10-26/` (outcome.md), `2026-10-31/` (outcome.md)
- The date folder was designed to handle re-applications, but in practice it's used as a timestamp for outcome updates

### Proposed Structure

```
# Before (4 levels)
thomson-reuters/lead-software-engineer-mobile-app/
├── 2026-09-27/
│   └── FrancoNColaizzi_Resume_ThompsonReuters.docx
├── 2026-10-21/
│   └── outcome.md
└── 2026-10-31/
    └── outcome.md

# After (3 levels, date-first prefix)
thomson-reuters/lead-software-engineer-mobile-app/
├── 2026-09-27_FrancoNColaizzi_Resume_ThompsonReuters.docx
├── 2026-10-21_outcome.md
└── 2026-10-31_outcome.md
```

### Design Principles

1. **Date-first prefix**: Files sort chronologically in `ls` and file browsers by default
2. **Underscore separator**: `YYYY-MM-DD_filename` (underscore between date and original name)
3. **OpenFonts subdirectories preserved**: `OpenFonts/` subdirectories within a role folder remain as-is (they are build artifacts, not dated artifacts)
4. **GCS mirrors local**: `applications/{company}/{role}/{date}_{filename}`
5. **Backward compatibility during migration**: A migration script handles the transition atomically

### Surfaces That Reference the Current Pattern

| Surface | Type | Change Needed |
|---------|------|---------------|
| `ArtifactMetadata.kt` | Kotlin — `blobPath()` | GCS path builder signature and output |
| `ApplyPipelineCoordinator.kt` | Kotlin — artifact upload | Uses `artifacts/$applicationId/` pattern, not `applications/` — no change needed |
| `ArtifactsCommand.kt` | Kotlin — CLI sync | Directory walk logic in `sync` subcommand |
| `.agents/commands/apply.md` | Command spec | ~20 references to folder path pattern |
| `.agents/commands/outcome.md` | Command spec | Archive folder conventions |
| `.agents/commands/interview.md` | Command spec | Reads from archive |
| `JOB-APPLICATION-STATES.md` | Documentation | Gate 2 re-sync invariant |
| `archive/lib/tracker/artifacts.py` | Archived Python | Frozen, no change needed |
| `archive/lib/artifacts.py` | Archived Python | Frozen, no change needed |
| `archive/scripts/artifacts.py` | Archived Python | Frozen, no change needed |
| `archive/scripts/blob_migrate.py` | Archived Python | Frozen, no change needed |
| `archive/scripts/blob_purge_legacy.py` | Archived Python | Frozen, no change needed |

## 3. Proposed Changes & Implementation Steps

### 3.1 Update `ArtifactUtils.blobPath()` in `ArtifactMetadata.kt`

**File**: `records/parser/src/main/kotlin/dev/fnc314/aijobtracker/records/parser/ArtifactMetadata.kt`

Change `blobPath()` to remove the date subdirectory and return a flattened path:

**Before:**
```kotlin
public fun blobPath(
    company: String,
    role: String,
    date: String,
    kind: String,
    filename: String,
): String {
    // ...
    return "applications/$normCompany/$normRole/$date/$filename"
}
```

**After:**
```kotlin
public fun blobPath(
    company: String,
    role: String,
    date: String,
    kind: String,
    filename: String,
): String {
    // ...
    val prefixedFilename = if (filename.startsWith(date + "_")) filename else "$date_$filename"
    return "applications/$normCompany/$normRole/$prefixedFilename"
}
```

The `date` parameter remains for backward compatibility with callers, but is now used to prefix the filename rather than create a subdirectory. The function checks if the filename already has the date prefix to avoid double-prefixing.

### 3.2 Update `ArtifactsCommand.kt` sync subcommand

**File**: `frontend/apps/cli/src/main/kotlin/dev/fnc314/aijobtracker/frontend/cli/commands/ArtifactsCommand.kt`

The `ArtifactsSyncCommand` walks the local directory tree to discover artifacts. The walk logic currently expects `company/role/date/file` depth. Update to expect `company/role/file` where filenames start with `YYYY-MM-DD_`.

Key changes:
- Update path segment parsing: `pathSegments` will have 1 segment (the filename) instead of 2 (date + filename)
- Extract date from filename prefix instead of directory name
- Update role folder detection to not confuse date-prefixed filenames with role folders

### 3.3 Create Migration Script

Create a migration script (as a `mise` task) that:

1. **Scans** all `job-applications/` directories
2. **Moves** each file from `company/role/date/filename` to `company/role/date_filename`
3. **Handles conflicts**: If `company/role/date_filename` already exists, append a counter (e.g., `date_filename (1)`)
4. **Removes** empty date directories
5. **Preserves** `OpenFonts/` subdirectories
6. **Dry-run mode**: Preview all changes without executing

The script should be a zsh script in `.config/mise/tasks/` following existing task conventions.

### 3.4 GCS Resync

After local migration, resync GCS to match:
1. Upload all renamed local files to new GCS paths
2. Delete old GCS blobs at the old paths
3. Verify checksums match

This can be done by running the existing `ArtifactsSyncCommand` after the local migration, or by a dedicated GCS migration step.

### 3.5 Update Command Documentation

Update all references to the folder path pattern in:

**`.agents/commands/apply.md`** (~20 occurrences):
- `job-applications/<company>/<role>/<YYYY-MM-DD>/` → `job-applications/<company>/<role>/`
- `job-applications/<Company>/<Role>/<YYYY-MM-DD>/outcome.md` → `job-applications/<Company>/<Role>/<YYYY-MM-DD>_outcome.md`
- `job-applications/<Company>/<Role>/<YYYY-MM-DD>/main_<company>_<role><CV_EXT>` → `job-applications/<Company>/<Role>/<YYYY-MM-DD>_main_<company>_<role><CV_EXT>`
- `job-applications/<Company>/<Role>/<YYYY-MM-DD>/cover_<company>_<role><COVER_EXT>` → `job-applications/<Company>/<Role>/<YYYY-MM-DD>_cover_<company>_<role><COVER_EXT>`
- Step 1: Remove "Create `job-applications/<company>/<role>/<YYYY-MM-DD>/` if it doesn't exist" — replace with "Create `job-applications/<company>/<role>/` if it doesn't exist"

**`.agents/commands/outcome.md`**:
- `job-applications/<Company>/<Role>/<YYYY-MM-DD>/` → `job-applications/<Company>/<Role>/`
- `job-applications/<Company>/<Role>/<YYYY-MM-DD>/outcome.md` → `job-applications/<Company>/<Role>/<YYYY-MM-DD>_outcome.md`

**`.agents/commands/interview.md`**:
- `job-applications/<Company>/<Role>/<YYYY-MM-DD>/` → `job-applications/<Company>/<Role>/`
- `job-applications/<Company>/<Role>/<YYYY-MM-DD>/interview_prep_<stage>.md` → `job-applications/<Company>/<Role>/<YYYY-MM-DD>_interview_prep_<stage>.md`

**`JOB-APPLICATION-STATES.md`**:
- Gate 2 re-sync invariant: `job-applications/<company>/<role>/<YYYY-MM-DD>/` → `job-applications/<company>/<role>/`

## 4. Verification Plan & Testing Checklist

### 4.1 Local Migration Verification
- [ ] Run migration script in dry-run mode and review all planned moves
- [ ] Execute migration and verify no files are lost (count before/after)
- [ ] Verify all date directories are removed (except `OpenFonts/` subdirs)
- [ ] Verify all filenames start with `YYYY-MM-DD_` prefix
- [ ] Verify chronological sorting works: `ls` output shows files in date order

### 4.2 GCS Resync Verification
- [ ] Verify GCS blob count matches local file count
- [ ] Verify a sample of GCS paths match the new format: `applications/{company}/{role}/{date}_{filename}`
- [ ] Verify old GCS paths are deleted

### 4.3 Code Verification
- [ ] `ArtifactUtils.blobPath()` returns correct flattened paths (unit test)
- [ ] `ArtifactsSyncCommand` correctly discovers artifacts in the new structure
- [ ] `mise run dev:test` passes (records test suite)
- [ ] `mise run dev:detekt` passes
- [ ] `mise run dev:konsist` passes

### 4.4 Documentation Verification
- [ ] All references to the old path pattern are updated in command files
- [ ] No remaining references to `job-applications/<Company>/<Role>/<YYYY-MM-DD>/` in `.agents/commands/`

### 4.5 Edge Cases
- [ ] Re-applications (same company, same role, different dates): files coexist with different date prefixes
- [ ] Multiple outcome updates: `2026-10-21_outcome.md` and `2026-10-31_outcome.md` coexist
- [ ] Files without a clear date (e.g. `cover.cls`): handled by migration script (skipped or assigned current date)
- [ ] `OpenFonts/` subdirectories: preserved, not migrated

## 5. Bookkeeping
- [ ] Plan persisted at `.agents/plans/2026-11-20/152149_artifact-directory-flatten-migration.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`