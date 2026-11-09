#!/usr/bin/env bash
#MISE description="Deploy Cloud Scheduler CRON job for routine sweeps (1 of 3 free jobs)"

set -euo pipefail

PROJECT="${REMOTE_GCP_PROJECT:-${TRACKER_PROJECT:-}}"
REGION="${REMOTE_GCP_REGION:-us-east1}"
CRON="${REMOTE_SWEEP_CRON:-0 */6 * * *}"
SERVICE_NAME="job-tracker-backend"

SERVICE_URL=$(gcloud run services describe "${SERVICE_NAME}" --platform=managed --region="${REGION}" --project="${PROJECT}" --format="value(status.url)")

if [ -z "$SERVICE_URL" ]; then
  echo "Error: Could not determine Cloud Run service URL." >&2
  exit 1
fi

JOB_NAME="job-tracker-routine-sweep"

echo "Configuring Cloud Scheduler job ${JOB_NAME} with CRON '${CRON}' -> ${SERVICE_URL}/tasks/sweep..."

if gcloud scheduler jobs describe "${JOB_NAME}" --location="${REGION}" --project="${PROJECT}" >/dev/null 2>&1; then
  gcloud scheduler jobs update http "${JOB_NAME}" \
    --location="${REGION}" \
    --project="${PROJECT}" \
    --schedule="${CRON}" \
    --uri="${SERVICE_URL}/tasks/sweep" \
    --http-method=POST
else
  gcloud scheduler jobs create http "${JOB_NAME}" \
    --location="${REGION}" \
    --project="${PROJECT}" \
    --schedule="${CRON}" \
    --uri="${SERVICE_URL}/tasks/sweep" \
    --http-method=POST
fi

echo "Cloud Scheduler configuration complete."
