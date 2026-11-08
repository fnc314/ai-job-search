#!/usr/bin/env sh
#MISE description="Run Konsist architectural test suites across records and ui/kmp"
#MISE alias="konsist"
set -eu
(cd records && ./gradlew :records-parser:test)
(cd ui/kmp && ./gradlew :apps-core:jvmTest)
