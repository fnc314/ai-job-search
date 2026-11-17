#!/usr/bin/env zsh
#MISE description="Generate the HTML applications report"
#MISE alias="report"
#USAGE flag "-o --output <output>" help="Output path for the HTML dashboard (default: ui/web/application-dashboard.html)"

set -euo pipefail

exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="report $*"
