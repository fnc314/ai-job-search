#!/usr/bin/env sh
#MISE description="Run Detekt static analysis on ui/kmp"
set -eu
cd ui/kmp && ./gradlew detekt
