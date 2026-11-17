#!/usr/bin/env zsh
#MISE description="Assemble Web JS distribution bundle"
set -eu
./gradlew :frontend:apps-web:jsBrowserDistribution
