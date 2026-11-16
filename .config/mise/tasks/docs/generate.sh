#!/usr/bin/env sh
#MISE description="Generate aggregated Dokka API documentation HTML from workspace root"
#MISE alias="docs:generate"
set -eu

exec "${MISE_PROJECT_ROOT}/gradlew" :dokkaGenerateHtml --stacktrace "$@"
