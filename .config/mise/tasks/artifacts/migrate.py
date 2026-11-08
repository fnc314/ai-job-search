#!/usr/bin/env python3
#MISE description="Upload local artifacts to GCS and backfill Firestore (set TRACKER_ALLOW_WRITES=1)"
#MISE alias="migrate-gcs"
#USAGE flag "--dry-run" help="Show tree view without uploading"
#USAGE flag "--company <company>" help="Only process artifacts for this company (case-insensitive)"
#USAGE flag "--documents <dir>" help="Path to job-applications/ directory"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    migrate_script = root / "scripts" / "migrate_gcs.py"
    import runpy
    sys.argv[0] = str(migrate_script)
    runpy.run_path(str(migrate_script), run_name="__main__")

if __name__ == "__main__":
    main()
