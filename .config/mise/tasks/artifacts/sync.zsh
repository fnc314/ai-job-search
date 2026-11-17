#!/usr/bin/env zsh
#MISE description="Sync local job-applications files into Firestore artifact subcollections"
#USAGE flag "--dry-run" help="Preview changes without writing"
#USAGE flag "--company <company>" help="Limit to a specific company directory"

set -euo pipefail

exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="artifacts sync $*"
