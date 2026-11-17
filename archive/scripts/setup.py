#!/usr/bin/env python3
"""DEPRECATED: Environment setup — retained as Python, simple utility.

Interactive minimum setup for the Firestore-backed job tracker.

This script deliberately provisions nothing by itself.  It validates a prepared
environment, seeds the schema only after confirmation, and can then populate
the *normal* tracker with namespaced mock records.  The mock path never calls
Composio: it writes an inbox-read JSON payload and passes it to ``collect``.

Use ``--check`` for a non-mutating readiness report.
"""

from __future__ import annotations

import argparse
import os
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
TRACKER = ROOT / "scripts" / "tracker.py"
MOCKS = ROOT / "tools" / "mock_data.py"


def loadEnvFile() -> None:
    """Load the repository's simple KEY=VALUE file for direct Python invocations.

    mise normally loads `.env.local`, but this keeps the documented
    `.venv/bin/python scripts/setup.py` command self-contained. Existing process
    environment values always win.
    """
    path = ROOT / ".env.local"
    if not path.exists():
        return
    for rawLine in path.read_text(encoding="utf-8").splitlines():
        line = rawLine.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip("\"'")
        if key:
            os.environ.setdefault(key, value)


def run(command: list[str]) -> int:
    """Run a repository command visibly and return its exit code."""
    print("\n$ " + " ".join(command))
    return subprocess.run(command, cwd=ROOT, check=False).returncode


def ask(prompt: str, *, default: bool = False) -> bool:
    """Ask a yes/no question without treating blank input as consent."""
    suffix = " [Y/n] " if default else " [y/N] "
    try:
        answer = input(prompt + suffix).strip().lower()
    except EOFError:
        return default
    if not answer:
        return default
    return answer in {"y", "yes"}


def environmentProblems(*, write: bool = True) -> list[str]:
    """Return configuration gaps that prevent a live tracker connection.

    When ``write=False`` (read-only ``--check``), the write-gate requirement
    is omitted so a readiness report works without ``TRACKER_ALLOW_WRITES``.
    """
    problems: list[str] = []
    if not (ROOT / ".env.local").exists():
        problems.append(".env.local is missing (copy .env.example first)")
    key = os.environ.get("TRACKER_SA_KEY")
    fallback = pathlib.Path.home() / ".config" / "job-tracker" / "sa.json"
    if key and not pathlib.Path(key).expanduser().exists():
        problems.append("TRACKER_SA_KEY points to a missing file")
    elif not key and not fallback.exists() and not os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"):
        problems.append("no service-account key or Application Default Credentials found")
    if write and os.environ.get("TRACKER_ALLOW_WRITES") != "1":
        problems.append("TRACKER_ALLOW_WRITES is not 1 (required only for seed/mock injection)")
    return problems


def showPrerequisites() -> None:
    print("\nMinimum configuration required before this script can seed or inject data:")
    print("  1. Copy .env.example to .env.local and set TRACKER_PROJECT, TRACKER_BUCKET,")
    print("     TRACKER_SA_KEY (or install ~/.config/job-tracker/sa.json), and TRACKER_ALLOW_WRITES=1.")
    print("  2. Create Firestore and the artifacts bucket as documented in SETUP.md.")
    print("  3. Share a Google Sheet with the service account only if you want --sheet sync.")
    print("\nNo Composio key is needed to seed or inject mock data.")


def check(*, write: bool = True) -> int:
    if not write:
        print("\nReadiness check (read-only — TRACKER_ALLOW_WRITES not required):")
    else:
        showPrerequisites()
    problems = environmentProblems(write=write)
    if problems:
        print("\nConfiguration gaps:")
        for problem in problems:
            print(f"  - {problem}")
        return 2
    return run([sys.executable, str(TRACKER), "check"])


def main(argv: list[str] | None = None) -> int:
    loadEnvFile()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="show readiness; never write")
    parser.add_argument("--sheet", action="store_true", help="sync the generated Sheet after mock injection")
    parser.add_argument("--count", type=int, default=84, help="number of synthetic applications")
    args = parser.parse_args(argv)

    if args.check:
        return check(write=False)

    ready = check(write=True)
    if ready:
        print("\nResolve the listed gaps, then re-run this script.")
        return ready
    if not ask("Seed or verify the tracker schema now?"):
        print("No changes made.")
        return 0
    if run([sys.executable, str(TRACKER), "seed"]):
        return 1
    if not ask("Inject namespaced mock applications and a mock inbox-read payload?"):
        print("Schema is ready. No mock data was added.")
        return 0

    mockDir = ROOT / "generated" / "mock"
    if run([sys.executable, str(MOCKS), "--out", str(mockDir), "--count", str(args.count)]):
        return 1
    if run([sys.executable, str(TRACKER), "import", "--csv", str(mockDir / "job_search_tracker.csv"), "--mock"]):
        return 1
    if run([sys.executable, str(TRACKER), "collect", "--json", str(mockDir / "gmail-inbox.json")]):
        return 1
    if args.sheet:
        if run([sys.executable, str(TRACKER), "sheet", "--sync"]):
            return 1
    print("\nMock data is ready. It uses only companies beginning with 'Mock '.")
    print("Preview removal:  .venv/bin/python scripts/tracker.py mock-clear")
    print("Remove mock data: .venv/bin/python scripts/tracker.py mock-clear --apply")
    return 0


if __name__ == "__main__":
    sys.exit(main())
