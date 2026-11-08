#!/usr/bin/env python3
#MISE description="Show a proposal and its proposed changes"
#USAGE arg "<id>" help="Proposal ID"

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
    sys.argv = [str(tracker_script), "proposals", "show"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
