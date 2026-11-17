#!/usr/bin/env zsh
#MISE description="Manage job applications — check status or manage drafts interactively"
#MISE alias="apply"
#USAGE arg "[url_or_jd]" help="Job URL or job description text (optional; omit for interactive draft management)"

set -euo pipefail

TARGET="${1:-}"

if [[ -n "$TARGET" ]]; then
    echo "================================================================================"
    echo "Job Target Provided: $TARGET"
    echo "================================================================================"
    echo ""
    echo "To evaluate fit, draft tailored LaTeX CV & Cover Letter artifacts, and compile PDFs,"
    echo "run the AI agent /apply command in your chat session (or via your agent CLI):"
    echo ""
    echo "  /apply $TARGET"
    echo ""
    echo "The /apply workflow executes:"
    echo "  1. Portal scrape / text extraction and candidate profile fit evaluation"
    echo "  2. Compensation & market salary benchmarking"
    echo "  3. Tailored LaTeX CV (templates/cv/main_example.tex) and Cover Letter (templates/cover-letters/cover.cls)"
    echo "  4. Multi-agent review & critique against ATS rules"
    echo "  5. PDF compilation and Firestore tracking alignment"
    echo ""
    echo "If you want to manage existing drafts instead, run without arguments: mise run search:apply"
else
    echo "[search:apply] Querying applications with status DRAFTING..."
    exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker list --status DRAFTING"
fi

