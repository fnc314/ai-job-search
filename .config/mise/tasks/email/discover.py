#!/usr/bin/env python3
#MISE description="Scan cached emails for untracked job applications and stage discovery proposals (set TRACKER_ALLOW_WRITES=1)"
#USAGE flag "--all" help="Scan all cached emails, not just recent ones"
#USAGE flag "--since <date>" help="Only scan emails since this date (YYYY-MM-DD)"
#USAGE flag "--dry-run" help="Preview discovery candidates without writing proposals"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()


def main() -> None:
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent

    scripts_dir = str(root / "scripts")
    lib_dir = str(root / "lib")
    for p in (scripts_dir, lib_dir, str(root)):
        if p not in sys.path:
            sys.path.insert(0, p)

    import argparse

    parser = argparse.ArgumentParser(description="Discover untracked job applications from email evidence")
    parser.add_argument("--all", action="store_true", help="Scan all cached emails")
    parser.add_argument("--since", type=str, help="Only scan emails since this date (YYYY-MM-DD)")
    parser.add_argument("--dry-run", action="store_true", help="Preview without writing")
    args = parser.parse_args()

    # Use the Kotlin BackendService via a Python wrapper script
    discover_script = root / "scripts" / "discover.py"
    sys.argv = [str(discover_script)] + sys.argv[1:]
    import runpy
    runpy.run_path(str(discover_script), run_name="__main__")


if __name__ == "__main__":
    main()
