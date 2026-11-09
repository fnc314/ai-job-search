#!/usr/bin/env bash
#MISE description="Deploy Cloud Run service and Cloud Scheduler CRON job in sequence"
#USAGE flag "--local" help="Build image locally via Docker"
#USAGE flag "--cloud" help="Build image remotely using Cloud Build"

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "${usage_local:-false}" = "true" ]; then
  "${DIR}/service.sh" --local
elif [ "${usage_cloud:-false}" = "true" ]; then
  "${DIR}/service.sh" --cloud
else
  "${DIR}/service.sh"
fi

"${DIR}/scheduler.sh"

echo "Complete remote pipeline deployed."
