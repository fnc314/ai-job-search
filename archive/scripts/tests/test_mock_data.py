"""Offline coverage for the namespaced mock-data generator."""

from __future__ import annotations

import csv
import json
import pathlib
import subprocess
import sys
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parent.parent
GENERATOR = ROOT / "mock_data.py"


class TestMockDataGenerator(unittest.TestCase):
    """Verify the mock generator produces namespaced, well-formed output."""

    def test_generator_creates_namespaced_tracker_and_inbox(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = pathlib.Path(tmp_dir)
            result = subprocess.run(
                [
                    sys.executable,
                    str(GENERATOR),
                    "--out",
                    str(tmp_path),
                    "--count",
                    "12",
                    "--reference-date",
                    "2026-10-31",
                ],
                cwd=ROOT,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            manifest = json.loads((tmp_path / "manifest.json").read_text())
            self.assertEqual(manifest["applications"], 12)
            with (tmp_path / "job_search_tracker.csv").open(newline="") as handle:
                rows = list(csv.DictReader(handle))
            self.assertEqual(len(rows), 12)
            for row in rows:
                self.assertTrue(row["company"].startswith("Mock "))
                self.assertTrue(row["id"].startswith("mock-"))
            inbox = json.loads((tmp_path / "gmail-inbox.json").read_text())
            self.assertNotIn("query", inbox)
            self.assertTrue(inbox["messages"])
            for message in inbox["messages"]:
                self.assertTrue(message["messageId"].startswith("mock"))


class TestSetupWizard(unittest.TestCase):
    """Verify setup.py exposes a read-only check mode."""

    def test_setup_wizard_has_read_only_check_mode(self):
        result = subprocess.run(
            [sys.executable, str(ROOT / "scripts" / "setup.py"), "--help"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(result.returncode, 0)
        self.assertIn("--check", result.stdout)


if __name__ == "__main__":
    unittest.main()
