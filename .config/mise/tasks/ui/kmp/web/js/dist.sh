#!/usr/bin/env sh
#MISE description="Assemble Web JS distribution bundle"
set -eu
cd ui/kmp && ./gradlew :apps-web:jsBrowserDistribution
