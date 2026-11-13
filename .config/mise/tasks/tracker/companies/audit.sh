#!/usr/bin/env bash
#MISE description="Audit Firestore companies collection to find and remove orphan/garbage records"
#USAGE flag "--dry-run" help="Preview orphan companies and garbage patterns without deleting"

set -euo pipefail

DRY_RUN="${usage_dry_run:-}"

if [ -n "${DRY_RUN}" ]; then
  echo "Running company audit in DRY-RUN mode..."
  "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run --args="--audit-companies --dry-run"
else
  if [ "${TRACKER_ALLOW_WRITES:-0}" != "1" ]; then
    echo "Error: TRACKER_ALLOW_WRITES=1 is required to delete garbage companies (or pass --dry-run)." >&2
    exit 1
  fi
  echo "Running company audit against Firestore (will delete garbage companies)..."
  "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run --args="--audit-companies"
fi
