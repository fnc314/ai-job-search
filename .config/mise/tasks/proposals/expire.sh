#!/usr/bin/env bash
#MISE description="Expire pending proposals that are past their dueAt timestamp"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="proposals expire $*"
