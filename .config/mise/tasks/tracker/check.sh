#!/usr/bin/env bash
#MISE description="Validate contract and connection"
#USAGE flag "-o --offline" help="Skip the Firestore reachability probe"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker check $*"
