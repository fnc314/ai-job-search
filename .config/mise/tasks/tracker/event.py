#!/usr/bin/env python3
#MISE description="Append a timeline event"
#USAGE arg "<id>" help="Application ID"
#USAGE flag "--type <type>" help="Event type (e.g. event:interview)"
#USAGE flag "--date <date>" help="ISO date the event happened"
#USAGE flag "--note <note>" help="Event note"
#USAGE flag "--json <json>" help="Extra fields as JSON"
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
    sys.argv = [str(tracker_script), "event"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
