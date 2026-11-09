#!/usr/bin/env sh
#MISE description="Build all composite subsystems across records, backend, and frontend"

set -eu
REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "${REPO_ROOT}"
./gradlew buildAll
