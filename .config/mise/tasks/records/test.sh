#!/usr/bin/env sh
#MISE description="Run unit and parser tests across records"
set -eu
exec "${MISE_PROJECT_ROOT}/gradlew" :records:records-parser:test "$@"
