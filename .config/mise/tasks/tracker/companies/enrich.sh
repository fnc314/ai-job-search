#!/usr/bin/env bash
#MISE description="Batch-enrich existing company records with missing metadata (sector, LinkedIn URL)"
#USAGE flag "--endpoint <url>" help="Override remote service endpoint URL"
#USAGE flag "--dry-run" help="Preview which companies need enrichment without writing"

set -euo pipefail

DRY_RUN="${usage_dry_run:-}"
ENDPOINT="${usage_endpoint:-${REMOTE_SERVICE_URL:-}}"

if [ -z "$DRY_RUN" ]; then
  # Try remote endpoint first
  if [ -z "$ENDPOINT" ]; then
    PROJECT="${REMOTE_GCP_PROJECT:-${TRACKER_PROJECT:-$(gcloud config get-value project 2>/dev/null || echo "job-search-fnc314-dev")}}"
    REGION="${REMOTE_GCP_REGION:-us-east1}"
    ENDPOINT=$(gcloud run services describe job-tracker-backend --platform=managed --region="${REGION}" --project="${PROJECT}" --format="value(status.url)" 2>/dev/null || true)
  fi

  if [ -n "$ENDPOINT" ]; then
    echo "Triggering batch company enrichment on ${ENDPOINT}..."
    curl -s -X POST "${ENDPOINT}/companies/tasks/enrich/batch" | jq . || true
    exit 0
  fi

  # Fallback to local execution
  echo "No remote endpoint found, running batch enrichment locally..."
  "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run --args="--enrich-batch"
else
  echo "Dry run mode — listing companies that need enrichment..."
  echo "(Requires HUNTER_IO_API_KEY for full enrichment; this preview only checks missing fields.)"
  "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run --args="--enrich-batch"
fi
