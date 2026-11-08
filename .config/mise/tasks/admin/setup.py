#!/usr/bin/env python3
#MISE description="Run first-time setup (validate environment, seed schema, inject mock records)"
#MISE alias="setup"
#USAGE flag "--check" help="Show readiness report without writing"
#USAGE flag "--sheet" help="Sync the Google Sheet after mock injection"
#USAGE flag "--count <count>" help="Number of synthetic applications to create"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    setup_script = root / "scripts" / "setup.py"
    import runpy
    sys.argv[0] = str(setup_script)
    runpy.run_path(str(setup_script), run_name="__main__")

if __name__ == "__main__":
    main()
