#!/usr/bin/env sh
#MISE description="Assemble Android debug APK (:apps-android:assembleDebug)"
#USAGE flag "--force-refresh" help="Force refresh dependencies" default="false"
#USAGE flag "--scan" help="add --scan to the gradle command" default="false"

set -eu

forceRefresh="${usage_force_refresh:-false}"
scan="${usage_scan:-false}"

# Clear the positional parameter list so we can use "$@" as our flags array
set --

if [ "${scan}" = "true" ]; then
  set -- "$@" "--scan"
fi

if [ "${forceRefresh}" = "true" ]; then
  set -- "$@" \
    "--no-build-cache" \
    "--no-configuration-cache" \
    "--refresh-dependencies"
fi

./gradlew :frontend:apps-android:assembleDebug "$@"
