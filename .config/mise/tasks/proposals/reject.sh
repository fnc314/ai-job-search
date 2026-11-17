#!/usr/bin/env bash
#MISE description="Reject a proposal with a reason"
#USAGE arg "<id>" help="Proposal ID"
#USAGE flag "--reason <reason>" help="Why it was rejected"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="proposals reject $*"
