#!/usr/bin/env python3
#MISE description="Register and upload an artifact file to GCS"
#USAGE arg "<id>" help="Application ID"
#USAGE flag "--kind <kind>" help="Artifact type (e.g. artifact:cv)"
#USAGE flag "--path <path>" help="Local file path"
#USAGE flag "--filename <filename>" help="Override stored filename"
#USAGE flag "--no-upload" help="Record the path only"
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
    sys.argv = [str(tracker_script), "artifact"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
