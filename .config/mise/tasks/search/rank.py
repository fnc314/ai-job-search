#!/usr/bin/env python3
#MISE description="Evaluate a job posting against the candidate profile (single-job fit score)"
#MISE alias="rank"
#USAGE arg "<url_or_jd>" help="Job URL, local file path, or job description text (required)"
#USAGE flag "--json" help="Output as JSON instead of markdown"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    rank_script = root / "scripts" / "rank.py"
    import runpy
    sys.argv[0] = str(rank_script)
    runpy.run_path(str(rank_script), run_name="__main__")

if __name__ == "__main__":
    main()
