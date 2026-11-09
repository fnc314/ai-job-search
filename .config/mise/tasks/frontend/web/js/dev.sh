#!/usr/bin/env sh
#MISE description="Run Web application in browser using JS development server"
set -eu
./gradlew :frontend:apps-web:jsBrowserDevelopmentRun
