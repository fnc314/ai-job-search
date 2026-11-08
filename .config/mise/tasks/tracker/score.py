#!/usr/bin/env python3
#MISE description="Record a recalculated fit rating"
#USAGE arg "<id>" help="Application ID"
#USAGE flag "--fit <fit>" help="Integer 0-100"
#USAGE flag "--date <date>" help="ISO date the score was decided"
#USAGE flag "--note <note>" help="What changed in the evaluation"
#USAGE flag "--dry-run" help="Preview without writing"

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
    sys.argv = [str(tracker_script), "score"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
