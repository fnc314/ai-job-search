#!/usr/bin/env zsh
#MISE description="Restore Firestore collections from a local JSON or NDJSON backup"
#USAGE arg "<file>" help="Path to backup JSON or NDJSON file"
#USAGE flag "--dry-run" help="Validate and preview without writing"

set -euo pipefail

exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker import $*"
