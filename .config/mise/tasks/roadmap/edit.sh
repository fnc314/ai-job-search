#!/usr/bin/env bash
#MISE description="Open the interactive roadmap manager (Kotlin TUI)"

set -euo pipefail

TUI_BIN="${MISE_PROJECT_ROOT}/frontend/apps/terminal/build/install/apps-terminal/bin/apps-terminal"

if [ ! -f "${TUI_BIN}" ]; then
  "${MISE_PROJECT_ROOT}/gradlew" :frontend:apps-terminal:installDist
fi

exec "${TUI_BIN}" --roadmap
