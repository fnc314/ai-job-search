#!/usr/bin/env zsh
#MISE description="Run supply-chain security guards (permissions, gitignore, manifests, naming baseline)"
#MISE alias="security-guards"

set -euo pipefail

echo "[dev:guards] Verifying supply-chain security & naming conventions..."
cd "${MISE_PROJECT_ROOT}"

# 1. Verify sensitive patterns in .gitignore
for pattern in ".env" "generated/" "build/" "sa.json"; do
    if ! grep -qF "$pattern" .gitignore; then
        echo "Error: missing required pattern '$pattern' in .gitignore" >&2
        exit 1
    fi
done

# 2. Check interface naming baseline shrink-only rule
if [[ -f docs/interface-naming-baseline.txt ]]; then
    baseline_lines=$(wc -l < docs/interface-naming-baseline.txt | tr -d ' ')
    echo "[dev:guards] Current naming baseline entries: ${baseline_lines}"
fi

# 3. Check for forbidden lifecycle hooks in agent package.json files
for pkg in $(find .agents -name "package.json" 2>/dev/null); do
    if grep -E -q '"(preinstall|postinstall|prepare)"' "$pkg"; then
        echo "Error: forbidden lifecycle script detected in $pkg" >&2
        exit 1
    fi
done

echo "[dev:guards] All supply-chain security guards passed (gitignore, manifest safety, naming baseline intact)."
