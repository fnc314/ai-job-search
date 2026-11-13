#!/usr/bin/env bash
#MISE description="Migrate zero-application companies to potentials collection"
#USAGE flag "--dry-run" help="Report only; do not modify Firestore" default="false"
#
# Without TRACKER_ALLOW_WRITES=1, runs in dry-run mode (report only).
# With TRACKER_ALLOW_WRITES=1, actually deletes garbage and migrates to potentials.

set -euo pipefail

DRY_RUN_FLAG=""
for arg in "$@"; do
  if [ "$arg" = "--dry-run" ]; then
    DRY_RUN_FLAG="--dry-run"
  fi
done

if [ "${TRACKER_ALLOW_WRITES:-0}" != "1" ] && [ -z "$DRY_RUN_FLAG" ]; then
  echo "Running migration in DRY-RUN mode (set TRACKER_ALLOW_WRITES=1 to execute)..."
  DRY_RUN_FLAG="--dry-run"
fi

if [ -n "$DRY_RUN_FLAG" ]; then
  echo "Running migration in DRY-RUN mode..."
else
  echo "⚠️  Executing migration with WRITE access. This will:"
  echo "  - Delete garbage companies from Firestore"
  echo "  - Move zero-app companies to the 'potentials' collection"
  echo "  - Keep motorola and motorola-solutions in companies"
fi

./gradlew -p backend run --args="--migrate-potentials ${DRY_RUN_FLAG}"
