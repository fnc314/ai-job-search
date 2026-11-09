#!/usr/bin/env bash
#MISE description="Run frontend unit and architectural tests (core-data, apps-core Konsist)"
set -euo pipefail

cd "${MISE_PROJECT_ROOT}"
exec "${MISE_PROJECT_ROOT}/gradlew" :frontend:core-data:jvmTest :frontend:apps-core:jvmTest "$@"
