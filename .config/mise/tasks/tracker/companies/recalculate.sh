#!/usr/bin/env bash
#MISE description="Recalculate company metrics locally and update Firestore directly"
#USAGE flag "--company-id <id>" help="Target company ID to recalculate (default: ALL)"

set -euo pipefail

COMPANY_ARG=""
if [ -n "${usage_company_id:-}" ]; then
  COMPANY_ARG="${usage_company_id}"
fi

echo "Recalculating company metrics locally (target: ${COMPANY_ARG:-ALL})..."
if [ -n "${COMPANY_ARG}" ]; then
  "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run --args="--recalculate ${COMPANY_ARG}"
else
  "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run --args="--recalculate"
fi
