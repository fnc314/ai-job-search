#!/usr/bin/env bash
#MISE description="Create an application from a JSON payload"
#USAGE flag "--json <file>" help="Payload file, or - for stdin"
#USAGE flag "-i --interactive" help="Prompt for fields interactively"
#USAGE flag "--dry-run" help="Resolve and validate only"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker add $*"
