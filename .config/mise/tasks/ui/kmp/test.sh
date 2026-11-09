#!/usr/bin/env sh
#MISE description="Run KMP unit tests across shared modules"
set -eu
cd ui/kmp
if [ "$#" -gt 0 ]; then
  ./gradlew "$@"
else
  ./gradlew jvmTest
fi

