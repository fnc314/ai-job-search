#!/usr/bin/env zsh
#MISE description="Assemble Web Wasm distribution bundle"
set -eu
./gradlew :frontend:apps-web:wasmJsBrowserDistribution
