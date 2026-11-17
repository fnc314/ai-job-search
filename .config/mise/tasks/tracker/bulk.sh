#!/usr/bin/env bash
#MISE description="Add many applications from one JSON file"
#USAGE flag "--file <file>" help="Path to JSON file"
#USAGE flag "--dry-run" help="Validate only without writing"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker bulk $*"
