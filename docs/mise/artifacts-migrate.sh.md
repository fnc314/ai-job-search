## `artifacts:migrate`

Upload local artifacts to GCS and backfill Firestore (set TRACKER_ALLOW_WRITES=1)


- **Usage:** `artifacts:migrate [FLAGS]`
- **Aliases:** `migrate-gcs`

### Flags
- **`--dry-run`** — Show tree view without uploading
- **`--company <company>`** — Only process artifacts for this company (case-insensitive)
- **`--documents <dir>`** — Path to job-applications/ directory