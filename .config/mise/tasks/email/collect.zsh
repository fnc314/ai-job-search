#!/usr/bin/env zsh
#MISE description="Stage proposals from fetched email messages to Firestore (set TRACKER_ALLOW_WRITES=1)"
#MISE alias="gmail:collect"
#USAGE flag "--json <json>" help="Inbox payload path (e.g. from Composio email fetch)"
#USAGE flag "--labels <labels>" help="JSON inventory of email provider labels/folders"
#USAGE flag "--trigger <trigger>" help="Trigger source: manual | cron | application-updated"
#USAGE flag "--dry-run" help="Preview without writing"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="email collect $*"
