#!/usr/bin/env zsh
#MISE description="Run Web application in browser using JS development server"
set -eu
./gradlew :frontend:apps-web:jsBrowserDevelopmentRun
