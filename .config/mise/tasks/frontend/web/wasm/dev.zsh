#!/usr/bin/env zsh
#MISE description="Run Web application in browser using Wasm development server"
set -eu
./gradlew :frontend:apps-web:wasmJsBrowserDevelopmentRun
