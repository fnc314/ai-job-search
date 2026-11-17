#!/usr/bin/env bash
#MISE description="Manage staged proposals (subcommands: list, show, approve, reject, expire)"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker proposals $*"
