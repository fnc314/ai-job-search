#!/usr/bin/env bash
#MISE description="Open the interactive roadmap manager (Kotlin TUI). Use ↑/↓ to navigate, Enter to inspect."
#USAGE arg "[subcommand]" help="i or e to jump to a specific item" {
#USAGE   choices "i" "e"
#USAGE }
#USAGE arg "[item-id]" help="Roadmap item ID (e.g., RM-001)"

set -euo pipefail

TUI_BIN="${MISE_PROJECT_ROOT}/internal/apps/terminal/roadmap/build/install/apps-terminal-roadmap/bin/apps-terminal-roadmap"

if [ ! -f "${TUI_BIN}" ]; then
  "${MISE_PROJECT_ROOT}/gradlew" :internal:apps-terminal-roadmap:installDist
fi

ARGS=()
[ -n "${usage_subcommand:-}" ] && ARGS+=("$usage_subcommand")
[ -n "${usage_item_id:-}" ] && ARGS+=("$usage_item_id")
export APPS_TERMINAL_ROADMAP_OPTS="--enable-native-access=ALL-UNNAMED"
exec "${TUI_BIN}" "${ARGS[@]}"
