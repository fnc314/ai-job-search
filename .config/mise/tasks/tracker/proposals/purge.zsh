#!/usr/bin/env zsh
#MISE description="Purge pending proposals from Firestore (requires TRACKER_ALLOW_WRITES=1)"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker proposals purge $*"
