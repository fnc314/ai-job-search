#!/usr/bin/env sh
#MISE description="Generate Dokka API documentation HTML for records"
#MISE alias="records:docs:generate"
set -eu
exec "${MISE_PROJECT_ROOT}/gradlew" :records:dokkaGenerate "$@"
