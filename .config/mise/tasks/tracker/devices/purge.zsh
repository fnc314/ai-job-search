#!/usr/bin/env zsh
#MISE description="Purge all registered push notification client devices (requires TRACKER_ALLOW_WRITES=1)"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker devices purge $*"
