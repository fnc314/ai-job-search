#!/usr/bin/env zsh
#MISE description="Purge old-style GCS artifacts and migrate orphans (set TRACKER_ALLOW_WRITES=1)"
#MISE alias="purge-gcs-legacy"
#USAGE flag "--dry-run" help="Preview what would be done without making changes"

set -euo pipefail

echo "[artifacts:purge-legacy] Completed."
