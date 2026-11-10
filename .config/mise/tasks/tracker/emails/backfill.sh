#!/usr/bin/env sh
#MISE description="Ingest and correlate emails from generated/gmail-inbox.json into Firestore emails collection"
#USAGE flag "--dry-run" help="Dry run without writing to Firestore"
#USAGE flag "--local" help="Execute directly via local backend runner rather than Cloud Run"

set -eu

BACKEND_BIN="${MISE_PROJECT_ROOT}/backend/build/install/backend/bin/backend"

if [ "${1:-}" = "--local" ] || [ -n "${TRACKER_LOCAL_BACKEND:-}" ]; then
  shift || true
  if [ ! -f "${BACKEND_BIN}" ]; then
    "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" installDist
  fi
  exec "${BACKEND_BIN}" --emails-backfill "$@"
fi

SERVICE_URL=$(gcloud run services describe jobtracker-backend --platform managed --region us-central1 --format 'value(status.url)' 2>/dev/null || echo "")

if [ -n "${SERVICE_URL}" ]; then
  TOKEN=$(gcloud auth print-identity-token 2>/dev/null || echo "")
  AUTH_HEADER=""
  if [ -n "${TOKEN}" ]; then
    AUTH_HEADER="Authorization: Bearer ${TOKEN}"
  fi

  DRY_RUN_PARAM=""
  if [ "${1:-}" = "--dry-run" ]; then
    DRY_RUN_PARAM="?dryRun=true"
  fi

  echo "Invoking remote Cloud Run email backfill at ${SERVICE_URL}..."
  curl -s -X POST "${SERVICE_URL}/emails/tasks/backfill${DRY_RUN_PARAM}" \
    -H "Content-Type: application/json" \
    ${AUTH_HEADER:+-H "${AUTH_HEADER}"}
  echo ""
else
  echo "Cloud Run service not detected. Falling back to local backend runner..."
  if [ ! -f "${BACKEND_BIN}" ]; then
    "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" installDist
  fi
  exec "${BACKEND_BIN}" --emails-backfill "$@"
fi
