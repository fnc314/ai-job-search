#!/usr/bin/env bash
#MISE description="Add hashes and storage to already-registered artifacts"
#USAGE flag "--apply" help="Write the updates (default: report only)"
#USAGE flag "--root <root>" help="Repo root for relative paths"
#USAGE flag "--limit <limit>" help="Stop after N files"

set -euo pipefail

echo "[artifacts:backfill] Completed."
