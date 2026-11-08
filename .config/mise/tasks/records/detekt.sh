#!/usr/bin/env sh
#MISE description="Run Detekt static analysis on records"
set -eu
cd records && ./gradlew detekt
