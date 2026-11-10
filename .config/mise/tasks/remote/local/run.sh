#!/usr/bin/env sh
#MISE description="Run backend Ktor service locally on http://127.0.0.1:8080"
#USAGE flag "--port <port>" help="Port to listen on (default: 8080)"

set -eu
PORT="${usage_port:-8080}"
PORT="${PORT}" exec "${MISE_PROJECT_ROOT}/gradlew" -p "${MISE_PROJECT_ROOT}/backend" run
