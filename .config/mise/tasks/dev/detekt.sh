#!/usr/bin/env sh
#MISE description="Run Detekt static analysis across records and frontend"
#MISE alias="detekt"
set -eu

cd "${MISE_PROJECT_ROOT}"
"${MISE_PROJECT_ROOT}/gradlew" :records:detekt "$@"
mise run frontend:detekt "$@"
