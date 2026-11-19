#!/usr/bin/env zsh
#MISE description="Manage job applications — evaluate fit, draft artifacts, and transition statuses via CLI"
#MISE alias="apply"
#USAGE arg "[url_or_jd]" help="Job URL, file path, or job description text (optional; omit to list RANKED and DRAFTING applications)"
#USAGE flag "--company <company>" help="Target company name override"
#USAGE flag "--role <role>" help="Target role title override"
#USAGE flag "--ai-rank" negate="--no-ai-rank" help="Run Tier 2 AI semantic calibration (default: enabled)"
#USAGE flag "--draft-assets" negate="--no-draft-assets" help="Generate tailored CV and cover letter sources"
#USAGE flag "--submitted" negate="--no-submitted" help="Mark application as SUBMITTED"
#USAGE flag "--persist" help="Persist status transitions to Firestore"
#USAGE flag "--output-dir <dir>" help="Root directory for generated artifacts"

set -euo pipefail

TARGET="${1:-}"

if [[ -n "$TARGET" ]]; then
    exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="apply $*"
else
    echo "[search:apply] Querying applications with status RANKED or DRAFTING..."
    "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker list --status RANKED"
    exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker list --status DRAFTING"
fi
