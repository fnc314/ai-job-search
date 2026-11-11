#!/usr/bin/env bash
#MISE description="Evaluate a job posting against the candidate profile (single-job fit score)"
#MISE alias="rank"
#USAGE arg "<url_or_jd>" help="Job URL, local file path, or job description text (required)"
#USAGE flag "--json" help="Output as JSON instead of markdown"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker rank $*"
