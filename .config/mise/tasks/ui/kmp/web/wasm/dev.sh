#!/usr/bin/env sh
#MISE description="Run Web application in browser using Wasm development server"
set -eu
cd ui/kmp && ./gradlew :apps-web:wasmJsBrowserDevelopmentRun
