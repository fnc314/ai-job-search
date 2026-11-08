#!/usr/bin/env sh
#MISE description="Assemble Web Wasm distribution bundle"
set -eu
cd ui/kmp && ./gradlew :apps-web:wasmJsBrowserDistribution
