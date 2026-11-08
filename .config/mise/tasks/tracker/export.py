#!/usr/bin/env python3
#MISE description="Read the application record back out"
#USAGE flag "--json" help="Full JSON payload"
#USAGE flag "--csv" help="Legacy CSV shape"
#USAGE flag "--ndjson" help="One application per line"
#USAGE flag "-o --output <output>" help="Write to file instead of stdout"
#USAGE flag "--refresh-cache" help="Also refresh tracker-cache.json"

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
    sys.argv = [str(tracker_script), "export"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
