#!/usr/bin/env bash
#MISE description="Record a recruiter outreach lead"
#USAGE flag "-c --company <company>" help="Company name (required)"
#USAGE flag "--channel <channel>" help="Channel value (default: outreach:linkedin)"
#USAGE flag "-r --contact <contact>" help="Recruiter or contact name"
#USAGE flag "-n --note <note>" help="Context about outreach" var=#true
#USAGE flag "-t --tag <tag>" help="Additional tags" var=#true
#USAGE flag "--dry-run" help="Resolve and validate only"

set -euo pipefail

gradle_args=("tracker" "lead")
for arg in "$@"; do
    gradle_args+=("$arg")
done

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="${gradle_args[*]}"

