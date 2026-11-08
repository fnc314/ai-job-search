#!/usr/bin/env python3
#MISE description="Single write path for the job search tracker store (subcommands: check, seed, list, get, add, upsert, lead, status, score, event, artifact, backfill, bulk, import, export, sheet, proposals, collect, labels, sync-due, mock-clear, edit, align)"
#MISE alias="tracker"

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
    sys.argv[0] = str(tracker_script)
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
