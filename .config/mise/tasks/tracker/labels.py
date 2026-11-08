#!/usr/bin/env python3
#MISE description="Show email folder labels seen at the last sync (from Gmail via Composio)"

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
    sys.argv = [str(tracker_script), "labels"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
