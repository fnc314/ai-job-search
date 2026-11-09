#!/usr/bin/env sh
#MISE description="Run Detekt static analysis on records"
set -eu
exec "${MISE_PROJECT_ROOT}/gradlew" :records:detekt "$@"
