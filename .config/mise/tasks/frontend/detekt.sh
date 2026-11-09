#!/usr/bin/env bash
#MISE description="Run Detekt static analysis across frontend modules"
set -eu

cd "${MISE_PROJECT_ROOT}"

declare -a TOP_LEVEL_DIRS=(apps core features)
declare -a FEATURE_SUB_MODULES=(data domain ui)

TASKS=()

for top_level in "${TOP_LEVEL_DIRS[@]}"; do
  case "$top_level" in
    core)
      for mod_dir in frontend/core/*/; do
        [ -d "$mod_dir" ] || continue
        mod_name=$(basename "$mod_dir")
        TASKS+=(":frontend:core-${mod_name}:detekt")
      done
      ;;
    features)
      for feat_dir in frontend/features/*/; do
        [ -d "$feat_dir" ] || continue
        feat_name=$(basename "$feat_dir")
        for sub in "${FEATURE_SUB_MODULES[@]}"; do
          if [ -d "${feat_dir}${sub}" ]; then
            TASKS+=(":frontend:features-${feat_name}-${sub}:detekt")
          fi
        done
      done
      ;;
    apps)
      for app_dir in frontend/apps/*/; do
        [ -d "$app_dir" ] || continue
        app_name=$(basename "$app_dir")
        if [ -f "${app_dir}build.gradle.kts" ] && grep -q -E "local\.kotlin\.multiplatform|local\.multiplatform\.library|detekt" "${app_dir}build.gradle.kts"; then
          TASKS+=(":frontend:apps-${app_name}:detekt")
        fi
      done
      ;;
  esac
done

exec "${MISE_PROJECT_ROOT}/gradlew" "${TASKS[@]}" "$@"
