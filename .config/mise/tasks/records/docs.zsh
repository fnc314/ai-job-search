#!/usr/bin/env zsh
#MISE description="Generate Dokka API documentation HTML for records"
#MISE alias="records:docs:generate"
set -eu
exec "${MISE_PROJECT_ROOT}/gradlew" :records:records-proto:dokkaGenerateHtml :records:records-parser:dokkaGenerateHtml "$@"
