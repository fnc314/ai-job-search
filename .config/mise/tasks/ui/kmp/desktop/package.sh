#!/usr/bin/env sh
#MISE description="Package Compose Desktop application for current OS"
set -eu
cd ui/kmp && ./gradlew :apps-desktop:packageDistributionForCurrentOS
