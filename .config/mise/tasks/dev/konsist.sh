#!/usr/bin/env bash
#MISE description="Run Konsist architectural test suites across records and frontend"
#MISE alias="konsist"
set -euo pipefail

cd "${MISE_PROJECT_ROOT}"
exec "${MISE_PROJECT_ROOT}/gradlew" :records:records-parser:test :frontend:apps-core:jvmTest "$@"
