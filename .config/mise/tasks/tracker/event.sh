#!/usr/bin/env bash
#MISE description="Add an event to an application's history"
#USAGE arg "<id>" help="Application ID (required)"
#USAGE arg "<event_type>" help="Event type (required)"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker event $*"
