#!/usr/bin/env bash
#MISE description="Get a single application by ID"
#USAGE arg "<id>" help="Application ID (required)"
#USAGE flag "--json" help="Output as JSON"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker get $*"
