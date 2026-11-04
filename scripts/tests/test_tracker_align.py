from __future__ import annotations

import pathlib
import sys
import tempfile
import unittest
from unittest.mock import MagicMock, patch

ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
if str(ROOT / "scripts") not in sys.path:
    sys.path.insert(0, str(ROOT / "scripts"))

import alignment
import contract
import tracker


class TestTrackerAlign(unittest.TestCase):
    def setUp(self):
        self.sample_app = {
            "id": "capital-one--mobile-engineer-4-android",
            "company": "Capital One",
            "companyId": "capital-one",
            "role": "Mobile Engineer 4, Android",
            "status": "status:applied",
            "date": "2026-11-02",
            "createdAt": "2026-11-02T22:36:11+00:00",
            "jobUrl": "https://capitalone.wd12.myworkdayjobs.com/test",
            "jobApplicationUrl": "https://capitalone.wd12.myworkdayjobs.com/test",
            "fitRating": 95,
            "uuid": "0192e1b4-7b00-7000-8000-000000000001",
            "location": {"city": "Remote"},
            "notesRendered": "2026-11-02 · event:note-added — C1 Role Meet and Greet",
        }

    def test_update_running_documentation_creates_and_updates_outcome_md(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            repo_root = pathlib.Path(tmpdir)
            events = [
                {
                    "type": "event:recruiter-screen",
                    "date": "2026-11-03",
                    "note": "phone screen with recruiter",
                }
            ]
            outcome_path = alignment.update_running_documentation(
                self.sample_app,
                events=events,
                repo_root=repo_root,
                dry_run=False,
            )
            self.assertIsNotNone(outcome_path)
            self.assertTrue(outcome_path.exists())

            content = outcome_path.read_text(encoding="utf-8")
            self.assertIn("# Job Application: Capital One — Mobile Engineer 4, Android", content)
            self.assertIn("https://capitalone.wd12.myworkdayjobs.com/test", content)
            self.assertIn("phone screen with recruiter", content)
            self.assertIn("## Initial Fit Evaluation", content)
            self.assertIn("Overall Fit Score:", content)

    @patch("alignment.cache.refresh")
    @patch("alignment.client.assertWritesAllowed")
    def test_align_migrates_mismatched_key_and_subcollections(self, mock_assert, mock_refresh):
        mock_db = MagicMock()
        mock_old_ref = MagicMock()
        mock_old_ref.id = "capital-one"

        mock_subcoll = MagicMock()
        mock_subcoll.id = "events"
        mock_subdoc = MagicMock()
        mock_subdoc.id = "event-1"
        mock_subdoc.to_dict.return_value = {
            "type": "event:applied",
            "date": "2026-11-02",
            "eventId": "event-1",
        }
        mock_subcoll.stream.return_value = [mock_subdoc]
        mock_old_ref.collections.return_value = [mock_subcoll]

        mock_snap = MagicMock()
        mock_snap.exists = True
        mock_snap.to_dict.return_value = dict(self.sample_app)
        mock_old_ref.get.return_value = mock_snap

        mock_target_ref = MagicMock()
        mock_target_ref.id = "capital-one--mobile-engineer-4-android"
        mock_db.collection.return_value.document.return_value = mock_target_ref

        mock_batch = MagicMock()
        mock_db.batch.return_value = mock_batch

        res = alignment.align_application(
            db=mock_db,
            identifier_or_ref=mock_old_ref,
            dry_run=False,
            recalc_fit=True,
            update_docs=False,
        )

        self.assertTrue(res["ok"])
        self.assertEqual(res["id"], "capital-one--mobile-engineer-4-android")
        self.assertTrue(res["migrated"])
        self.assertEqual(res["previousKey"], "capital-one")

        # Verify batch set called for target_ref and subcollection
        self.assertTrue(mock_batch.set.called)
        self.assertTrue(mock_batch.commit.called)
        mock_refresh.assert_called_once_with(mock_db)

    @patch("tracker.alignment.align_application")
    @patch("tracker.client.getDb")
    def test_command_edit_triggers_data_alignment(self, mock_get_db, mock_align):
        mock_db = MagicMock()
        mock_ref = MagicMock()
        mock_ref.id = "capital-one"
        mock_snap = MagicMock()
        mock_snap.exists = True
        app_data = dict(self.sample_app)
        app_data["role"] = "Old Role"
        mock_snap.to_dict.return_value = app_data
        mock_ref.get.return_value = mock_snap
        mock_db.collection.return_value.document.return_value = mock_ref
        mock_get_db.return_value = mock_db

        mock_align.return_value = {
            "ok": True,
            "id": "capital-one--mobile-engineer-4-android",
            "migrated": True,
            "fitRating": 95,
            "outcomeFile": "/path/to/outcome.md",
        }

        args = MagicMock(
            app_id="capital-one",
            id=None,
            interactive=False,
            dry_run=False,
            company=None,
            role="Mobile Engineer 4, Android",
            status=None,
            date=None,
            channel=None,
            contact_person=None,
            contact_email=None,
            job_url="https://capitalone.wd12.myworkdayjobs.com/new",
            job_application_url="https://capitalone.wd12.myworkdayjobs.com/new",
            fit_rating=None,
            location=None,
            sector=None,
            role_type=None,
            source=None,
            note="Update role and URLs",
        )

        with patch("tracker.emit") as mock_emit:
            ret = tracker.commandEdit(args)
            self.assertEqual(ret, tracker.EXIT_OK)
            self.assertTrue(mock_align.called)
            emitted = mock_emit.call_args[0][0]
            self.assertTrue(emitted["ok"])
            self.assertEqual(emitted["id"], "capital-one--mobile-engineer-4-android")
            self.assertTrue(emitted["aligned"])


if __name__ == "__main__":
    unittest.main()
