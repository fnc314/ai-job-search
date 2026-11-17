"""ARCHIVED: Mise-only enforcement guard. No longer needed as Python is removed from the project.

Runtime guard enforcing Mise-only invocation.

Any direct execution of Python binaries (python, python3, .venv/bin/python)
outside of a running Mise task will be halted with exit code 1.
"""
from __future__ import annotations

import os
import sys


def enforce_mise() -> None:
    """Terminates execution if not running inside a Mise task."""
    if not os.environ.get("MISE_TASK") and not os.environ.get("ALLOW_DIRECT_PYTHON"):
        sys.stderr.write(
            "\n[ERROR] Direct Python execution is strictly disallowed in this repository.\n"
            "All operations must be invoked via Mise:\n"
            "  -> Use 'mise run <task>' or 'mise <task>'\n"
            "  -> Run 'mise tasks ls' to view available tasks.\n\n"
        )
        sys.exit(1)
