#!/usr/bin/env bash
#MISE description="Approve a proposal and apply changes"
#USAGE arg "[id]" help="Proposal ID (optional with -i)"
#USAGE flag "-y --yes" help="Skip interactive confirmation prompts"
#USAGE flag "--note <note>" help="Resolution text"
#USAGE flag "--only <only>" help="Comma-separated change indexes to approve"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="proposals approve $*"
