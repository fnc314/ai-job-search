#!/usr/bin/env zsh
#MISE description="Delete namespaced mock records"
#USAGE flag "--apply" help="Actually delete (default: dry run)"

set -euo pipefail

exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker mock-clear $*"
