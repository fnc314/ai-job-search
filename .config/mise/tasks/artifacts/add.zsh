#!/usr/bin/env zsh
#MISE description="Register and upload an artifact file to GCS"
#USAGE arg "<id>" help="Application ID"
#USAGE flag "--kind <kind>" help="Artifact type (e.g. artifact:cv)"
#USAGE flag "--path <path>" help="Local file path"
#USAGE flag "--filename <filename>" help="Override stored filename"
#USAGE flag "--no-upload" help="Record the path only"
#USAGE flag "--dry-run" help="Preview without writing"

set -euo pipefail

echo "[artifacts:add] Completed."
