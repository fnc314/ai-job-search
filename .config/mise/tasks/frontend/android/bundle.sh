#!/usr/bin/env sh
#MISE description="Bundle Android release AAB (:apps-android:bundleRelease)"
#USAGE flag "--force-refresh" help="Force refresh dependencies" default="false"

set -eu

forceRefresh="${usage_force_refresh:-false}"

if [ "${forceRefresh}" = "true" ]; then
	./gradlew :frontend:apps-android:bundleRelease --no-build-cache --no-configuration-cache --refresh-dependencies
else
	./gradlew :frontend:apps-android:bundleRelease
fi
