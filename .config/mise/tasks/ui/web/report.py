#!/usr/bin/env python3
#MISE description="Generate the HTML applications report"
#MISE alias="report"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    report_script = root / "scripts" / "generate_report.py"
    import runpy
    sys.argv[0] = str(report_script)
    runpy.run_path(str(report_script), run_name="__main__")

if __name__ == "__main__":
    main()
