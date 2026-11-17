#!/usr/bin/env bash
#MISE description="Create-or-merge from a JSON payload"
#USAGE flag "--json <file>" help="Payload file, or - for stdin"
#USAGE flag "--dry-run" help="Resolve and validate only"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker upsert $*"
