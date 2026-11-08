#!/usr/bin/env sh
#MISE description="Run Detekt static analysis across records and ui/kmp"
#MISE alias="detekt"
set -eu
(cd records && ./gradlew detekt)
(cd ui/kmp && ./gradlew detekt)
