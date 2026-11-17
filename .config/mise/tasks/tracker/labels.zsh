#!/usr/bin/env zsh
#MISE description="Show email folder labels seen at the last sync (from Gmail via Composio)"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker labels $*"
