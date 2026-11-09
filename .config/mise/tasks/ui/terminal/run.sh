#!/usr/bin/env sh
#MISE description="Open the terminal job-tracker dashboard (Kotlin Mordant TUI)"
#MISE alias="tui"

set -eu
REPO_ROOT="$(git rev-parse --show-toplevel)"
TUI_BIN="${REPO_ROOT}/frontend/apps/terminal/build/install/apps-terminal/bin/apps-terminal"

if [ ! -f "${TUI_BIN}" ]; then
  "${REPO_ROOT}/frontend/gradlew" -p "${REPO_ROOT}/frontend" :apps-terminal:installDist
fi

exec "${TUI_BIN}" "$@"
