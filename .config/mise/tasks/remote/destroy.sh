#!/usr/bin/env bash
#MISE description="Tear down Cloud Run service and Cloud Scheduler job (guarantee $0 cost)"

set -euo pipefail

PROJECT="${REMOTE_GCP_PROJECT:-${TRACKER_PROJECT:-}}"
REGION="${REMOTE_GCP_REGION:-us-east1}"
SERVICE_NAME="job-tracker-backend"
JOB_NAME="job-tracker-routine-sweep"

echo "Tearing down remote deployment in ${REGION} (${PROJECT})..."

gcloud scheduler jobs delete "${JOB_NAME}" --location="${REGION}" --project="${PROJECT}" --quiet || true
gcloud run services delete "${SERVICE_NAME}" --platform=managed --region="${REGION}" --project="${PROJECT}" --quiet || true

echo "Remote teardown complete."
