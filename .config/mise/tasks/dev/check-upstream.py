#!/usr/bin/env python3
#MISE description="Preview upstream framework updates against personalized local files"
#USAGE flag "--remote <remote>" help="Remote name"
#USAGE flag "--branch <branch>" help="Branch name"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    check_script = root / "scripts" / "check_upstream_updates.py"
    import runpy
    sys.argv[0] = str(check_script)
    runpy.run_path(str(check_script), run_name="__main__")

if __name__ == "__main__":
    main()
