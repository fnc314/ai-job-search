#!/usr/bin/env python3
#MISE description="Convert salary data spreadsheet to benchmark JSON"
#USAGE arg "<file>" help="Path to excel file"
#USAGE flag "--source <source>" help="Source name"
#USAGE flag "--baseline <baseline>" help="Baseline number"
#USAGE flag "--baseline-desc <desc>" help="Baseline description"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    convert_script = root / "scripts" / "convert_salary_excel.py"
    import runpy
    sys.argv[0] = str(convert_script)
    runpy.run_path(str(convert_script), run_name="__main__")

if __name__ == "__main__":
    main()
