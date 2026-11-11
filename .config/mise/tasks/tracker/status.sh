#!/usr/bin/env bash
#MISE description="Update the status of an application"
#USAGE arg "<id>" help="Application ID (required)"
#USAGE arg "<status>" help="New status (required)"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker status $*"
