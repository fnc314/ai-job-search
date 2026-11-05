#!/usr/bin/env bash
# scripts/check-kmp-readmes.sh
# Verifies that all core and feature modules in ui/kmp have valid README.md documentation.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KMP_ROOT="${REPO_ROOT}/ui/kmp"

MODULES=(
    "core/data"
    "core/design-system"
    "core/network"
    "core/widgets"
    "features/sign-in"
    "features/job-applications-list"
    "features/job-applications-details"
    "features/settings"
    "features/add-job-application"
    "features/tracker-edits"
)

ERRORS=0

echo "🔍 Checking ui/kmp module README.md documentation..."

for mod in "${MODULES[@]}"; do
    readme_path="${KMP_ROOT}/${mod}/README.md"
    if [[ ! -f "${readme_path}" ]]; then
        echo "❌ Missing documentation: ${mod}/README.md"
        ERRORS=$((ERRORS + 1))
    elif [[ ! -s "${readme_path}" ]]; then
        echo "❌ Empty documentation: ${mod}/README.md"
        ERRORS=$((ERRORS + 1))
    else
        # Verify essential sections
        if ! grep -q "Quick Facts" "${readme_path}"; then
            echo "⚠️  ${mod}/README.md missing 'Quick Facts' section"
        fi
        if ! grep -q "Key Gradle" "${readme_path}"; then
            echo "⚠️  ${mod}/README.md missing 'Key Gradle' section"
        fi
        echo "✅ Found valid README: ${mod}/README.md"
    fi
done

if [[ ${ERRORS} -gt 0 ]]; then
    echo ""
    echo "❌ Validation failed: ${ERRORS} module(s) missing valid README.md files."
    exit 1
fi

echo ""
echo "🎉 All ${#MODULES[@]} ui/kmp module READMEs are present and validated."
exit 0
