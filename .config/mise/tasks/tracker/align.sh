#!/usr/bin/env bash
#MISE description="Align application document IDs and recalculate fit scores"
#USAGE arg "[id]" help="Optional target application ID to align a single record"
#USAGE flag "--dry-run" help="Report only without writing changes"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker align $*"
