#!/usr/bin/env bash
#MISE description="Show a proposal and its proposed changes"
#USAGE arg "<id>" help="Proposal ID"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="proposals show $*"
