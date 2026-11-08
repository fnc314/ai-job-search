#!/usr/bin/env python3
#MISE description="Record a recruiter outreach lead"
#USAGE flag "-c --company <company>" help="Company name (required)"
#USAGE flag "--channel <channel>" help="Channel value (default: outreach:linkedin)"
#USAGE flag "-r --contact <contact>" help="Recruiter or contact name"
#USAGE flag "-n --note <note>" help="Context about outreach" var=#true
#USAGE flag "-t --tag <tag>" help="Additional tags" var=#true
#USAGE flag "--dry-run" help="Resolve and validate only"

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
    sys.argv = [str(tracker_script), "lead"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
