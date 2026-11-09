#!/usr/bin/env sh
#MISE description="Clean Gradle build artifacts in frontend"
set -eu
./gradlew :frontend:clean
