#!/usr/bin/env python3
#MISE description="Serve and preview the web dashboard on localhost"
#MISE alias="dashboard"
#USAGE flag "-p --port <port>" help="Port to listen on (default: 8000)"
#USAGE flag "-b --host <host>" help="Host address to bind to (default: 127.0.0.1)"
#USAGE flag "-r --regenerate" help="Regenerate dashboard HTML from tracker before starting"
#USAGE flag "--no-browser" help="Do not automatically open the browser"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    server_script = root / "ui" / "web" / "application-dashboard-server.py"
    import runpy
    sys.path.insert(0, str(root / "ui" / "web"))
    sys.argv[0] = str(server_script)
    runpy.run_path(str(server_script), run_name="__main__")

if __name__ == "__main__":
    main()
