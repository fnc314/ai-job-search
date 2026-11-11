#!/usr/bin/env sh
#MISE description="Install Android debug APK on connected device (:apps-android:installDebug)"
#USAGE flag "--force-refresh" help="Force refresh dependencies" default="false"

set -eu

forceRefresh="${usage_force_refresh:-false}"

if [ "${forceRefresh}" = "true" ]; then
	./gradlew :frontend:apps-android:installDebug --no-build-cache --no-configuration-cache --refresh-dependencies
else
	./gradlew :frontend:apps-android:installDebug
fi
