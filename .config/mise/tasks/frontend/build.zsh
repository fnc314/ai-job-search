#!/usr/bin/env zsh
#MISE description="Build all frontend modules"
set -eu
./gradlew :frontend:build -x test
