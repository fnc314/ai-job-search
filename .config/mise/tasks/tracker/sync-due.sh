#!/usr/bin/env bash
#MISE description="Run due sync-requests"
#USAGE flag "--json <json>" help="Inbox payload for every request"
#USAGE flag "--dry-run" help="List what is due"

set -euo pipefail

exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker sync-due $*"
