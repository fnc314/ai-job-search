#!/usr/bin/env bash
#MISE description="Run Web application in browser using Compose Wasm development server"
#MISE alias="dashboard"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" :frontend:apps-web:wasmJsBrowserDevelopmentRun
