#!/usr/bin/env sh
#MISE description="Run Konsist architectural test suites across records and frontend"
#MISE alias="konsist"
set -eu
./gradlew :records:records-parser:test :frontend:apps-core:jvmTest
