#!/usr/bin/env zsh
#MISE description="Regenerate the Google Sheets view"
#USAGE flag "--sync" help="Accepted for symmetry"
#USAGE flag "--sheet-id <sheet_id>" help="Spreadsheet ID"
#USAGE flag "--local-only" help="Write CSVs locally"
#USAGE flag "--directory <directory>" help="Where --local-only writes"

set -euo pipefail

exec "${MISE_PROJECT_ROOT}/gradlew" -q :frontend:apps-cli:run --args="tracker sheet $*"
