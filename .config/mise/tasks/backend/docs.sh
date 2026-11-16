#!/usr/bin/env sh
#MISE description="Generate Dokka API documentation HTML for backend"
#MISE alias="backend:docs:generate"
set -eu
exec "${MISE_PROJECT_ROOT}/gradlew" :backend:service:dokkaGenerateHtml "$@"
