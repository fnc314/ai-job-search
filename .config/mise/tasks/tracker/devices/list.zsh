#!/usr/bin/env zsh
#MISE description="List registered push notification client devices"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker devices list $*"
