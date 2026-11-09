#!/usr/bin/env bash
#MISE description="Deploy Cloud Run service (supporting --local Docker build or --cloud Cloud Build)"
#USAGE flag "--local" help="Build image locally via Docker and push directly"
#USAGE flag "--cloud" help="Build image remotely using Google Cloud Build"

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
PROJECT="${REMOTE_GCP_PROJECT:-${TRACKER_PROJECT:-}}"
REGION="${REMOTE_GCP_REGION:-us-east1}"
MODE="${REMOTE_BUILD_MODE:-local}"

if [ "${usage_local:-false}" = "true" ]; then
  MODE="local"
elif [ "${usage_cloud:-false}" = "true" ]; then
  MODE="cloud"
fi

SERVICE_NAME="job-tracker-backend"
IMAGE_URI="${REGION}-docker.pkg.dev/${PROJECT}/job-tracker-backend/backend:latest"

echo "Building and deploying ${SERVICE_NAME} to ${REGION} in project ${PROJECT} (mode: ${MODE})..."

if [ "$MODE" = "local" ]; then
  "${REPO_ROOT}/frontend/gradlew" -p "${REPO_ROOT}/backend" installDist
  docker build -t "${IMAGE_URI}" "${REPO_ROOT}/backend"
  docker push "${IMAGE_URI}"
else
  gcloud builds submit --tag "${IMAGE_URI}" "${REPO_ROOT}/backend" --project="${PROJECT}"
fi

gcloud run deploy "${SERVICE_NAME}" \
  --image="${IMAGE_URI}" \
  --region="${REGION}" \
  --project="${PROJECT}" \
  --platform=managed \
  --allow-unauthenticated \
  --min-instances=0 \
  --max-instances=1 \
  --memory=512Mi \
  --cpu=1

echo "Deployment complete."
