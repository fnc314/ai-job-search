#!/usr/bin/env python3
#MISE description="Regenerate the Google Sheets view"
#USAGE flag "--sync" help="Accepted for symmetry"
#USAGE flag "--sheet-id <sheet_id>" help="Spreadsheet ID"
#USAGE flag "--local-only" help="Write CSVs locally"
#USAGE flag "--directory <directory>" help="Where --local-only writes"

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
    sys.argv = [str(tracker_script), "sheet"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
