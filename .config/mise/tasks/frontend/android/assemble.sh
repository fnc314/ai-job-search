#!/usr/bin/env sh
#MISE description="Assemble Android debug APK (:apps-android:assembleDebug)"
#USAGE flag "--force-refresh" help="Force refresh dependencies" default="false"

set -eu

forceRefresh="${usage_force_refresh:-false}"

if [ "${forceRefresh}" = "true" ]; then
	./gradlew :frontend:apps-android:assembleDebug --no-build-cache --no-configuration-cache --refresh-dependencies
else
	./gradlew :frontend:apps-android:assembleDebug
fi
