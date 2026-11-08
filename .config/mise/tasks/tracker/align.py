#!/usr/bin/env python3
#MISE description="Align applications in Firebase to canonical IDs, recalculate fit, and update outcome.md"
#MISE alias="align"
#USAGE arg "[id]" help="Application ID or UUID to align"
#USAGE flag "--app-id <app_id>" help="Application ID or UUID to align"
#USAGE flag "--all" help="Scan and align all applications"
#USAGE flag "--dry-run" help="Preview changes without writing"
#USAGE flag "--no-fit-recalc" help="Skip recalculating fit"
#USAGE flag "--no-docs-update" help="Skip updating outcome.md"
#USAGE flag "--note <note>" help="Optional audit note"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    tracker_script = root / "scripts" / "tracker.py"
    import runpy
    sys.argv = [str(tracker_script), "align"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
