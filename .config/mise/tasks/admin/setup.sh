#!/usr/bin/env bash
#MISE description="Run first-time setup (validate environment, seed schema, inject mock records)"
#MISE alias="setup"
#USAGE flag "--check" help="Show readiness report without writing"
#USAGE flag "--sheet" help="Sync the Google Sheet after mock injection"
#USAGE flag "--count <count>" help="Number of synthetic applications to create"

set -euo pipefail

exec "${{MISE_PROJECT_ROOT}}/gradlew" -q :frontend:apps-cli:run --args="tracker check"
