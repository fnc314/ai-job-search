#!/usr/bin/env zsh
#MISE description="One-shot import of legacy tracker CSV"
#USAGE flag "--csv <csv>" help="Path to legacy tracker CSV"
#USAGE flag "--dry-run" help="Report only, never write"
#USAGE flag "--mock" help="Skip historical profile reconciliation"
#USAGE flag "--report <report>" help="Write the reconciliation report"

set -euo pipefail

exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker import $*"
