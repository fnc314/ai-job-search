#!/usr/bin/env python3
#MISE description="Purge old-style GCS artifacts and migrate orphans (set TRACKER_ALLOW_WRITES=1)"
#MISE alias="purge-gcs-legacy"
#USAGE flag "--dry-run" help="Preview what would be done without making changes"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    purge_script = root / "scripts" / "purge_gcs_legacy.py"
    import runpy
    sys.argv[0] = str(purge_script)
    runpy.run_path(str(purge_script), run_name="__main__")

if __name__ == "__main__":
    main()
