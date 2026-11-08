#!/usr/bin/env sh
#MISE description="Bundle Android release AAB (:apps-android:bundleRelease)"
set -eu
cd ui/kmp && ./gradlew :apps-android:bundleRelease
