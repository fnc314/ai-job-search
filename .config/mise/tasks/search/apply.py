#!/usr/bin/env python3
#MISE description="Manage job applications — check status or manage drafts interactively"
#MISE alias="apply"
#USAGE arg "[url_or_jd]" help="Job URL or job description text (optional; omit for interactive draft management)"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    apply_script = root / "scripts" / "apply.py"
    import runpy
    sys.argv[0] = str(apply_script)
    runpy.run_path(str(apply_script), run_name="__main__")

if __name__ == "__main__":
    main()
