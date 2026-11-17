#!/usr/bin/env bash
#MISE description="Modify fields of an existing application record"
#USAGE arg "[id]" help="Application ID or UUID to edit"
#USAGE flag "--company <company>" help="New company name"
#USAGE flag "--role <role>" help="New role title"
#USAGE flag "--status <status>" help="New funnel status"
#USAGE flag "--channel <channel>" help="Application channel"
#USAGE flag "--job-url <job_url>" help="Job posting URL"
#USAGE flag "--note <note>" help="Optional context note for the event"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker edit $*"
