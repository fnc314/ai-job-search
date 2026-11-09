#!/usr/bin/env sh
#MISE description="Generate Dokka API documentation HTML for frontend"
#MISE alias="frontend:dokka"
set -eu
./gradlew :frontend:dokkaGenerate
