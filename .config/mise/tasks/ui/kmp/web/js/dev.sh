#!/usr/bin/env sh
#MISE description="Run Web application in browser using JS development server"
set -eu
cd ui/kmp && ./gradlew :apps-web:jsBrowserDevelopmentRun
