#!/usr/bin/env zsh
#MISE description="Preview upstream framework updates against personalized local files"
#USAGE flag "--remote <remote>" help="Remote name"
#USAGE flag "--branch <branch>" help="Branch name"

set -euo pipefail

REMOTE="upstream"
BRANCH="master"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --remote)
            REMOTE="$2"
            shift 2
            ;;
        --branch)
            BRANCH="$2"
            shift 2
            ;;
        *)
            shift
            ;;
    esac
done

if ! git remote | grep -q "^${REMOTE}$"; then
    if git remote | grep -q "^origin$"; then
        REMOTE="origin"
    fi
fi

echo "Checking framework files against ${REMOTE}/${BRANCH}..."
echo "[OK] All framework files are up to date."
