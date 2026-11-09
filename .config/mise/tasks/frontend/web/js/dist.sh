#!/usr/bin/env sh
#MISE description="Assemble Web JS distribution bundle"
set -eu
./gradlew :frontend:apps-web:jsBrowserDistribution
