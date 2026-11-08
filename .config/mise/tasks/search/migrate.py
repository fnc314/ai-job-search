#!/usr/bin/env python3
#MISE description="One-time migration: seed existing seen_jobs.json into Firestore"
#MISE alias="scrape:migrate"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    scrape_script = root / "scripts" / "scrape.py"
    import runpy
    sys.argv = [str(scrape_script), "--migrate-seen"]
    runpy.run_path(str(scrape_script), run_name="__main__")

if __name__ == "__main__":
    main()
