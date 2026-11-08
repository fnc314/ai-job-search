#!/usr/bin/env python3
#MISE description="Look up salary benchmark data"
#MISE alias="salary"
#USAGE arg "[company]" help="Company name to search for"
#USAGE flag "--city <city>" help="Filter by city name"
#USAGE flag "--json" help="Output as JSON"
#USAGE flag "--list-all" help="List all companies"
#USAGE flag "--validate" help="Validate salary_data.json and print a report, then exit"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    salary_script = root / "scripts" / "salary_lookup.py"
    import runpy
    sys.argv[0] = str(salary_script)
    runpy.run_path(str(salary_script), run_name="__main__")

if __name__ == "__main__":
    main()
