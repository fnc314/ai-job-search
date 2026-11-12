#!/usr/bin/env python3
#MISE description="Run due sync-requests"
#USAGE flag "--json <json>" help="Inbox payload for every request"
#USAGE flag "--dry-run" help="List what is due"

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
    scripts_dir = str(root / "scripts")
    # Remove this wrapper's directory from sys.path so it doesn't shadow
    # modules (e.g. 'proposals') that tracker.py imports from scripts/.
    wrapper_dir = str(Path(__file__).resolve().parent)
    sys.path = [p for p in sys.path if p != wrapper_dir]
    # Ensure scripts/ is first so tracker.py finds its sibling modules.
    if scripts_dir not in sys.path:
        sys.path.insert(0, scripts_dir)
    import runpy
    sys.argv = [str(tracker_script), "sync-due"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
