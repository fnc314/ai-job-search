#!/usr/bin/env bash
#MISE description="Trigger on-demand routine sweep on remote Cloud Run service"
#USAGE flag "--endpoint <url>" help="Override remote service endpoint URL"

set -euo pipefail

ENDPOINT="${usage_endpoint:-${REMOTE_SERVICE_URL:-}}"

if [ -z "$ENDPOINT" ]; then
  echo "Error: REMOTE_SERVICE_URL is not set in environment and no --endpoint was provided." >&2
  exit 1
fi

echo "Triggering routine sweep on ${ENDPOINT}..."
curl -s -X POST "${ENDPOINT}/tasks/sweep" | jq . || true
