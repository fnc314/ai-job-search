#!/usr/bin/env sh
#MISE description="Build all targets and modules in records"
set -eu
cd records && ./gradlew build
