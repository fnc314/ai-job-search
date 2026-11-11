#!/usr/bin/env bash
#MISE description="List applications with optional filters"
#USAGE flag "--status <status>" help="Filter by status"
#USAGE flag "--channel <channel>" help="Filter by channel"
#USAGE flag "--company <company>" help="Filter by company"
#USAGE flag "--json" help="Output as JSON"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker list $*"
