#!/usr/bin/env zsh
#MISE description="Scan cached emails for untracked job applications and stage discovery proposals (set TRACKER_ALLOW_WRITES=1)"
#USAGE flag "--all" help="Scan all cached emails, not just recent ones"
#USAGE flag "--since <date>" help="Only scan emails since this date (YYYY-MM-DD)"
#USAGE flag "--dry-run" help="Preview discovery candidates without writing proposals"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="email discover $*"
