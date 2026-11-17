#!/usr/bin/env zsh
#MISE description="Run the Compose Desktop (JVM) application"
set -eu
./gradlew :frontend:apps-desktop:run
