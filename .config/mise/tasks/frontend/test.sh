#!/usr/bin/env sh
#MISE description="Run frontend unit tests"
set -eu
./gradlew :frontend:core-data:jvmTest
