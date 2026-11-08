#!/usr/bin/env python3
#MISE description="One-shot import of legacy tracker CSV"
#USAGE flag "--csv <csv>" help="Path to legacy tracker CSV"
#USAGE flag "--dry-run" help="Report only, never write"
#USAGE flag "--mock" help="Skip historical profile reconciliation"
#USAGE flag "--report <report>" help="Write the reconciliation report"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    tracker_script = root / "scripts" / "tracker.py"
    import runpy
    sys.argv = [str(tracker_script), "import"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
