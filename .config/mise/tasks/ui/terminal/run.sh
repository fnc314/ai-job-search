#!/usr/bin/env sh
#MISE description="Open the terminal job-tracker dashboard (Kotlin Mordant TUI)"
#MISE alias="tui"

set -eu
TUI_BIN="${MISE_PROJECT_ROOT}/frontend/apps/terminal/build/install/apps-terminal/bin/apps-terminal"

if [ ! -f "${TUI_BIN}" ]; then
  "${MISE_PROJECT_ROOT}/gradlew" :frontend:apps-terminal:installDist
fi

exec "${TUI_BIN}" "$@"
