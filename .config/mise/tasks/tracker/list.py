#!/usr/bin/env python3
#MISE description="List applications"
#USAGE flag "--status <status>" help="Filter by status"
#USAGE flag "--open" help="Only open applications"
#USAGE flag "--tag <tag>" help="Filter by an exact tag value" var=#true
#USAGE flag "--limit <limit>" help="Max results to return"
#USAGE flag "--json" help="Output as JSON"
#USAGE flag "--tui" help="Launch interactive TUI with these filters"

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
    sys.argv = [str(tracker_script), "list"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
