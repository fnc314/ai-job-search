#!/usr/bin/env sh
#MISE description="Correlate and backfill historical email IDs into JobApplication.emailMessageIds"
#USAGE flag "--apply" help="Write updates to Firestore (default: dry run)"
#USAGE flag "--state-file <path>" help="Path to gmail-sync/state.json"
#USAGE flag "--inbox-file <path>" help="Path to generated/gmail-inbox.json"
#USAGE flag "--job-labels-file <path>" help="Path to generated/gmail-job-labels.json"

set -eu
CLI_BIN="${MISE_PROJECT_ROOT}/frontend/apps/cli/build/install/apps-cli/bin/apps-cli"

if [ ! -f "${CLI_BIN}" ]; then
  "${MISE_PROJECT_ROOT}/gradlew" :frontend:apps-cli:installDist
fi

exec "${CLI_BIN}" tracker backfill emails "$@"
