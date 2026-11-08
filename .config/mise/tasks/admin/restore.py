#!/usr/bin/env python3
#MISE description="Restore Firestore collections from a local JSON backup"
#USAGE arg "<file>" help="Path to backup JSON file"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    restore_script = root / "scripts" / "restore_firestore.py"
    import runpy
    sys.argv[0] = str(restore_script)
    runpy.run_path(str(restore_script), run_name="__main__")

if __name__ == "__main__":
    main()
