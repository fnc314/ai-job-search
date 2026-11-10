#!/usr/bin/env python3
#MISE description="Run Web application in browser using Compose Wasm development server"
#MISE alias="dashboard"

import subprocess
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    gradlew = root / "gradlew"
    cmd = [str(gradlew), ":frontend:apps-web:wasmJsBrowserDevelopmentRun"]
    sys.exit(subprocess.call(cmd))

if __name__ == "__main__":
    main()
