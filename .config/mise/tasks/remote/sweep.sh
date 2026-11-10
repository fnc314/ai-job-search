#!/usr/bin/env bash
#MISE description="Trigger on-demand routine sweep on remote Cloud Run service"
#USAGE flag "--endpoint <url>" help="Override remote service endpoint URL"

set -euo pipefail

ENDPOINT="${usage_endpoint:-${REMOTE_SERVICE_URL:-}}"

if [ -z "$ENDPOINT" ]; then
  PROJECT="${REMOTE_GCP_PROJECT:-${TRACKER_PROJECT:-$(gcloud config get-value project 2>/dev/null || echo "job-search-fnc314-dev")}}"
  REGION="${REMOTE_GCP_REGION:-us-east1}"
  ENDPOINT=$(gcloud run services describe job-tracker-backend --platform=managed --region="${REGION}" --project="${PROJECT}" --format="value(status.url)" 2>/dev/null || true)
fi

if [ -z "$ENDPOINT" ]; then
  echo "Error: REMOTE_SERVICE_URL is not set in environment and no --endpoint was provided." >&2
  exit 1
fi

echo "Triggering routine sweep on ${ENDPOINT}..."
curl -s -X POST "${ENDPOINT}/tasks/sweep" | jq . || true
