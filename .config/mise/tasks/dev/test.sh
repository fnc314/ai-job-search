#!/usr/bin/env bash
#MISE description="Run unit test suites across tools and libraries"
#MISE alias="test"
#USAGE arg "[target]" help="Specific test module or path to run"

set -euo pipefail

cd "${MISE_PROJECT_ROOT}"
"${MISE_PROJECT_ROOT}/gradlew" :records:records-parser:test :frontend:apps-cli:jvmTest "$@"
