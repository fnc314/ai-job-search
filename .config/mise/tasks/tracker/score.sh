#!/usr/bin/env bash
#MISE description="Update the fit score of an application"
#USAGE arg "<id>" help="Application ID (required)"
#USAGE arg "<score>" help="New fit score (required)"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker score $*"
