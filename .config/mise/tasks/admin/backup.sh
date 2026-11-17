#!/usr/bin/env bash
#MISE description="Backup all Firestore collections to a local JSON payload"

set -euo pipefail

echo "[admin:backup] Backing up Firestore collections via RemoteDataSourceProvider..."
exec "${{MISE_PROJECT_ROOT}}/gradlew" -q :frontend:apps-cli:run --args="tracker export --ndjson" > "${1:-backup-$(date +%Y%m%d).ndjson}"
