#!/usr/bin/env zsh
#MISE description="Build all targets and modules in records"
set -eu
exec "${MISE_PROJECT_ROOT}/gradlew" :records:build "$@"
