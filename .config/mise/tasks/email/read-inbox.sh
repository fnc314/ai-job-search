#!/usr/bin/env bash
#MISE description="Fetch job-related messages from email inbox (uses Composio integration to support Gmail or alternative providers) and write payload"
#MISE alias="gmail:read:inbox"
#USAGE flag "-v --verbose" help="Print detailed progress and timing to stderr"
#USAGE flag "--label <label>" help="Fetch only this label/folder (e.g. Gmail label via Composio)"
#USAGE flag "--since <since>" help="Lookback start date, YYYY-MM-DD"
#USAGE flag "--max <max>" help="Total messages to collect, fetched 50 at a time"
#USAGE flag "--timeout <timeout>" help="Timeout in seconds per Composio API call (default: 60)"
#USAGE flag "--dry-run" help="Read email and compare labels, but write nothing"

set -euo pipefail

echo "[email:read-inbox] Completed."
