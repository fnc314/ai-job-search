#!/usr/bin/env sh
#MISE description="Package Compose Desktop application for current OS"
set -eu
./gradlew :frontend:apps-desktop:packageDistributionForCurrentOS
