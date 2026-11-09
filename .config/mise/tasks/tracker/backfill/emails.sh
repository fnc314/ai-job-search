#!/usr/bin/env sh
#MISE description="Correlate and backfill historical email IDs into JobApplication.emailMessageIds"
#USAGE flag "--apply" help="Write updates to Firestore (default: dry run)"
#USAGE flag "--state-file <path>" help="Path to gmail-sync/state.json"

set -eu
REPO_ROOT="$(git rev-parse --show-toplevel)"
CLI_BIN="${REPO_ROOT}/frontend/apps/cli/build/install/apps-cli/bin/apps-cli"

if [ ! -f "${CLI_BIN}" ]; then
  "${REPO_ROOT}/frontend/gradlew" -p "${REPO_ROOT}/frontend" :apps-cli:installDist
fi

exec "${CLI_BIN}" tracker backfill emails "$@"
