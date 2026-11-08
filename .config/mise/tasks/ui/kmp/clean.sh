#!/usr/bin/env sh
#MISE description="Clean Gradle build artifacts in ui/kmp"
set -eu
cd ui/kmp && ./gradlew clean
