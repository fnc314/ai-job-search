#!/usr/bin/env bash
#MISE description="List proposals in the collector queue"
#USAGE flag "--state <state>" help="Filter by state (e.g. proposal:pending)"
#USAGE flag "--json" help="Output as JSON"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="proposals list $*"
