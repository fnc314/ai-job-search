#!/usr/bin/env sh
#MISE description="Run the Compose Desktop (JVM) application"
set -eu
cd ui/kmp && ./gradlew :apps-desktop:run
