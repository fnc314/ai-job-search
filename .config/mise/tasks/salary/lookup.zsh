#!/usr/bin/env zsh
#MISE description="Look up salary benchmark data"
#MISE alias="salary"
#USAGE arg "[company]" help="Company name to search for"
#USAGE flag "--city <city>" help="Filter by city name"
#USAGE flag "--json" help="Output as JSON"
#USAGE flag "--list-all" help="List all companies"
#USAGE flag "--validate" help="Validate salary data file and print a report, then exit"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="salary $*"
