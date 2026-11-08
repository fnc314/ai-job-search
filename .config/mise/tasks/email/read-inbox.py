#!/usr/bin/env python3
#MISE description="Fetch job-related messages from email inbox (uses Composio integration to support Gmail or alternative providers) and write payload"
#MISE alias="gmail:read:inbox"
#USAGE flag "-v --verbose" help="Print detailed progress and timing to stderr"
#USAGE flag "--label <label>" help="Fetch only this label/folder (e.g. Gmail label via Composio)"
#USAGE flag "--since <since>" help="Lookback start date, YYYY-MM-DD"
#USAGE flag "--max <max>" help="Total messages to collect, fetched 50 at a time"
#USAGE flag "--timeout <timeout>" help="Timeout in seconds per Composio API call (default: 60)"
#USAGE flag "--dry-run" help="Read email and compare labels, but write nothing"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    fetch_script = root / "scripts" / "gmail_fetch.py"
    import runpy
    sys.argv[0] = str(fetch_script)
    runpy.run_path(str(fetch_script), run_name="__main__")

if __name__ == "__main__":
    main()
