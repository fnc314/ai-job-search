#!/usr/bin/env python3
#MISE description="Open the terminal job-tracker dashboard (use keyboard shortcuts)"
#MISE alias="tui"
#MISE env={ TRACKER_SOURCE = "store" }

import os
import sys
from pathlib import Path

# Enforce Mise context
import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    tui_script = root / "ui" / "terminal" / "tracker_tui.py"
    # Execute the tracker_tui module
    import runpy
    sys.path.insert(0, str(root / "ui" / "terminal"))
    runpy.run_path(str(tui_script), run_name="__main__")

if __name__ == "__main__":
    main()
