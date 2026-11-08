#!/usr/bin/env sh
#MISE description="Build all KMP targets and modules in ui/kmp"
set -eu
cd ui/kmp && ./gradlew build -x test
