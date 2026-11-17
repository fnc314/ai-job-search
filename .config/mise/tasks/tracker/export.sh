#!/usr/bin/env bash
#MISE description="Read the application record back out"
#USAGE flag "--json" help="Full JSON payload"
#USAGE flag "--csv" help="Legacy CSV shape"
#USAGE flag "--ndjson" help="One application per line"
#USAGE flag "-o --output <output>" help="Write to file instead of stdout"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker export $*"
