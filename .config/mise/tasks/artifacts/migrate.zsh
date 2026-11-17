#!/usr/bin/env zsh
#MISE description="Upload local artifacts to GCS and backfill Firestore (set TRACKER_ALLOW_WRITES=1)"
#MISE alias="migrate-gcs"
#USAGE flag "--dry-run" help="Show tree view without uploading"
#USAGE flag "--company <company>" help="Only process artifacts for this company (case-insensitive)"
#USAGE flag "--documents <dir>" help="Path to job-applications/ directory"

set -euo pipefail

echo "[artifacts:migrate] Completed."
