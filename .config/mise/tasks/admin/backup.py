#!/usr/bin/env python3
#MISE description="Backup all Firestore collections to a local JSON payload"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    backup_script = root / "scripts" / "backup_firestore.py"
    import runpy
    sys.argv[0] = str(backup_script)
    runpy.run_path(str(backup_script), run_name="__main__")

if __name__ == "__main__":
    main()
