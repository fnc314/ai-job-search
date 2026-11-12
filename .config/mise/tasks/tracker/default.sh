#!/usr/bin/env bash
#MISE description="Single write path for the job search tracker store (subcommands: check, seed, list, get, add, edit, merge, reclassify, status, score, event, align, rank, backfill)"
#MISE alias="tracker"

set -euo pipefail

"${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker $*"
