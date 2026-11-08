#!/usr/bin/env python3
#MISE description="Run unit test suites across tools and libraries"
#MISE alias="test"
#USAGE arg "[target]" help="Specific test module or path to run"

import os
import sys
import unittest
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    if len(sys.argv) > 1 and sys.argv[1]:
        # Run specific target
        unittest.main(module=None, argv=sys.argv)
    else:
        # Discover tests
        loader = unittest.TestLoader()
        start_dir = str(root / "scripts" / "tests")
        suite = loader.discover(start_dir, pattern="test_*.py", top_level_dir=str(root))
        runner = unittest.TextTestRunner(verbosity=2)
        result = runner.run(suite)
        sys.exit(0 if result.wasSuccessful() else 1)

if __name__ == "__main__":
    main()
