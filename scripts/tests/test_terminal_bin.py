"""Smoke and contract tests for frontend/apps/terminal binary and tracker hand-off."""

import json
import os
import subprocess
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
TUI_BIN = (
    REPO_ROOT
    / "frontend"
    / "apps"
    / "terminal"
    / "build"
    / "install"
    / "apps-terminal"
    / "bin"
    / "apps-terminal"
)


class TerminalBinTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Ensure binary exists
        if not TUI_BIN.exists():
            subprocess.run(
                [str(REPO_ROOT / "gradlew"), ":frontend:apps-terminal:installDist"],
                check=True,
                cwd=str(REPO_ROOT),
            )

    def test_binary_exists(self):
        self.assertTrue(TUI_BIN.exists(), f"Binary should exist at {TUI_BIN}")

    def test_proposals_flag_emits_json_summary(self):
        env = os.environ.copy()
        env["TRACKER_SOURCE"] = "cache"
        proc = subprocess.run(
            [str(TUI_BIN), "--proposals"],
            capture_output=True,
            text=True,
            env=env,
            cwd=str(REPO_ROOT),
        )
        self.assertEqual(proc.returncode, 0, f"Failed with {proc.stderr}")
        stdout_lines = [line.strip() for line in proc.stdout.splitlines() if line.strip()]
        self.assertTrue(len(stdout_lines) > 0, "Expected stdout output")
        last_line = stdout_lines[-1]
        summary = json.loads(last_line)
        self.assertTrue(summary.get("ok"))
        self.assertIn("remaining_pending", summary)
        self.assertEqual(summary.get("summary"), "proposals review complete")

    def test_list_open_with_cache(self):
        env = os.environ.copy()
        env["TRACKER_SOURCE"] = "cache"
        proc = subprocess.run(
            [str(TUI_BIN), "--open", "list"],
            capture_output=True,
            text=True,
            env=env,
            cwd=str(REPO_ROOT),
        )
        self.assertEqual(proc.returncode, 0, f"Failed with {proc.stderr}")
        self.assertIn("Job Application Pipeline", proc.stdout)

    def test_tracker_cli_proposals_approve_interactive(self):
        env = os.environ.copy()
        env["TRACKER_SOURCE"] = "cache"
        proc = subprocess.run(
            ["python3", "scripts/tracker.py", "proposals", "approve", "-i"],
            capture_output=True,
            text=True,
            env=env,
            cwd=str(REPO_ROOT),
        )
        self.assertEqual(proc.returncode, 0, f"Failed with stderr: {proc.stderr}")
        summary = json.loads(proc.stdout.strip())
        self.assertTrue(summary.get("ok"))
