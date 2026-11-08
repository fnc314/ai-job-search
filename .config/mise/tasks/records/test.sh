#!/usr/bin/env sh
#MISE description="Run unit and parser tests across records"
set -eu
cd records && ./gradlew test
