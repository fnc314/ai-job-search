#!/usr/bin/env python3
#MISE description="Stage proposals from fetched email messages to Firestore (set TRACKER_ALLOW_WRITES=1)"
#MISE alias="gmail:collect"
#USAGE flag "--json <json>" help="Inbox payload path (e.g. from Composio email fetch)"
#USAGE flag "--labels <labels>" help="JSON inventory of email provider labels/folders"
#USAGE flag "--trigger <trigger>" help="Trigger source: manual | cron | application-updated"
#USAGE flag "--dry-run" help="Preview without writing"

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
    sys.argv = [str(tracker_script), "collect"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
