#!/usr/bin/env python3
#MISE description="Fetch and cache email folder/labels (originates from Gmail labels, managed via Composio integration to enable swapping email providers)"
#MISE alias="gmail:sync:labels"
#USAGE flag "-v --verbose" help="Print detailed progress and timing to stderr"
#USAGE flag "--label <label>" help="Fetch only this label/folder (validated against email provider labels)"
#USAGE flag "--since <since>" help="Lookback start date, YYYY-MM-DD"
#USAGE flag "--timeout <timeout>" help="Timeout in seconds per Composio email API call (default: 60)"

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
    sys.argv = [str(fetch_script), "--dry-run"] + sys.argv[1:]
    runpy.run_path(str(fetch_script), run_name="__main__")

if __name__ == "__main__":
    main()
