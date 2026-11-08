#!/usr/bin/env python3
#MISE description="Run supply-chain security guards (permissions, gitignore, manifests)"
#MISE alias="security-guards"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    guards_script = root / "scripts" / "security_guards.py"
    import runpy
    sys.argv[0] = str(guards_script)
    runpy.run_path(str(guards_script), run_name="__main__")

if __name__ == "__main__":
    main()
