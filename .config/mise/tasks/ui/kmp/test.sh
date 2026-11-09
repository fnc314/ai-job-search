#!/usr/bin/env sh
#MISE description="Run KMP unit tests across shared modules"
set -eu
cd ui/kmp && ./gradlew jvmTest
