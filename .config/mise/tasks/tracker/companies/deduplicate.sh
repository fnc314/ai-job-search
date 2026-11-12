#!/usr/bin/env bash
#MISE description="Deduplicate Firestore companies collection and align applications.companyId to canonical slugs"
#USAGE flag "--dry-run" help="Preview planned company merges and application updates without mutating Firestore"

set -euo pipefail

DRY_RUN="${usage_dry_run:-}"

if [ -n "${DRY_RUN}" ]; then
  echo "Running companies deduplication in DRY-RUN mode..."
  "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run --args="--deduplicate-companies --dry-run"
else
  if [ "${TRACKER_ALLOW_WRITES:-0}" != "1" ]; then
    echo "Error: TRACKER_ALLOW_WRITES=1 is required to mutate Firestore (or pass --dry-run)." >&2
    exit 1
  fi
  echo "Running companies deduplication against Firestore..."
  "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run --args="--deduplicate-companies"
fi
