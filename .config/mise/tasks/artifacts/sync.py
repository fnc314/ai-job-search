#!/usr/bin/env python3
#MISE description="Sync local job-applications files into Firestore artifact subcollections"
#USAGE flag "--dry-run" help="Preview changes without writing"
#USAGE flag "--company <company>" help="Limit to a specific company directory"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    script = root / "scripts" / "sync_local_artifacts.py"
    import runpy
    sys.argv = [str(script)] + sys.argv[1:]
    runpy.run_path(str(script), run_name="__main__")

if __name__ == "__main__":
    main()
