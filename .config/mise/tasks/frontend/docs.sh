#!/usr/bin/env sh
#MISE description="Generate Dokka API documentation HTML for frontend"
#MISE alias="frontend:dokka"
set -eu
exec "${MISE_PROJECT_ROOT}/gradlew" :frontend:apps-core:dokkaGenerateHtml "$@"
