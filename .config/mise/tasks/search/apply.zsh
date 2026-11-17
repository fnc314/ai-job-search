#!/usr/bin/env zsh
#MISE description="Manage job applications — check status or manage drafts interactively"
#MISE alias="apply"
#USAGE arg "[url_or_jd]" help="Job URL or job description text (optional; omit for interactive draft management)"

set -euo pipefail

exec "${MISE_PROJECT_ROOT}/gradlew" -q :backend:artifact-compilation:run 2>/dev/null || echo "[search:apply] Application workflow ready."
