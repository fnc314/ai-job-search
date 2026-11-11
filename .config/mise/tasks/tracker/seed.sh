#!/usr/bin/env bash
#MISE description="Seed the tracker with mock data for development"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker seed $*"
