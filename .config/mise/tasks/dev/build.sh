#!/usr/bin/env sh
#MISE description="Build all composite subsystems across records, backend, and frontend"

set -eu
cd "${MISE_PROJECT_ROOT}"
exec "${MISE_PROJECT_ROOT}/gradlew" buildAll "$@"
