"""Tests for `tracker edit` subcommand, URL sanitization, and UUID/ID resolution."""

from __future__ import annotations

import pathlib
import sys
import unittest
from unittest.mock import MagicMock, patch

ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
if str(ROOT / "scripts") not in sys.path:
    sys.path.insert(0, str(ROOT / "scripts"))

import contract  # noqa: E402
import tracker   # noqa: E402
import urls      # noqa: E402


class TestTrackerEdit(unittest.TestCase):
    def setUp(self):
        self.sample_app = {
            "id": "acme--staff-android-engineer",
            "uuid": "0192e1b4-7b00-7000-8000-000000000001",
            "company": "Acme",
            "companyId": "acme",
            "role": "Staff Android Engineer",
            "status": "status:applied",
            "statusRank": 2,
            "isOpen": True,
            "date": "2026-10-30",
            "schemaVersion": 7,
            "createdAt": "2026-10-30T00:00:00-04:00",
            "updatedAt": "2026-10-30T00:00:00-04:00",
            "jobUrl": None,
            "jobApplicationUrl": None,
            "tags": [],
            "contactIds": [],
            "metadata": {},
        }

    def test_kebab_to_camel(self):
        self.assertEqual(tracker.kebab_to_camel("job-application-url"), "jobApplicationUrl")
        self.assertEqual(tracker.kebab_to_camel("job-url"), "jobUrl")
        self.assertEqual(tracker.kebab_to_camel("contact-email"), "contactEmail")
        self.assertEqual(tracker.kebab_to_camel("company"), "company")

    def test_parse_dynamic_flags(self):
        extra = [
            "--",
            "--job-application-url=https://acme.com/apply?utm_source=li",
            "--job-url", "https://acme.com/job?fbclid=123",
            "--contact-person=Jane Doe",
        ]
        flags = tracker.parse_dynamic_flags(extra)
        self.assertEqual(
            flags,
            {
                "jobApplicationUrl": "https://acme.com/apply?utm_source=li",
                "jobUrl": "https://acme.com/job?fbclid=123",
                "contactPerson": "Jane Doe",
            },
        )

    def test_disallows_modifying_unique_ids(self):
        args = MagicMock(app_id="acme--staff-android-engineer", id=None, interactive=False, dry_run=True)
        with self.assertRaises(contract.ContractError) as ctx:
            tracker.commandEdit(args, extra_args=["--id=new-id"])
        self.assertIn("cannot be modified", str(ctx.exception))

        with self.assertRaises(contract.ContractError) as ctx:
            tracker.commandEdit(args, extra_args=["--uuid=0192e1b4-7b00-7000-8000-000000000002"])
        self.assertIn("cannot be modified", str(ctx.exception))

    @patch("tracker.client.getDb")
    def test_edit_urls_strips_tracking_parameters_and_dry_run(self, mock_get_db):
        mock_doc_ref = MagicMock()
        mock_snap = MagicMock()
        mock_snap.exists = True
        mock_snap.to_dict.return_value = dict(self.sample_app)
        mock_doc_ref.get.return_value = mock_snap

        mock_db = MagicMock()
        mock_db.collection.return_value.document.return_value = mock_doc_ref
        mock_get_db.return_value = mock_db

        args = MagicMock(
            app_id="acme--staff-android-engineer",
            id=None,
            interactive=False,
            dry_run=True,
            company=None,
            role=None,
            status=None,
            date=None,
            channel=None,
            contact_person=None,
            contact_email=None,
            job_url="https://acme.com/jobs/123?fbclid=track123&gh_jid=999",
            job_application_url="https://acme.com/apply?utm_source=linkedin&utm_campaign=hiring&valid=true",
            fit_rating=None,
            location=None,
            sector=None,
            role_type=None,
            source=None,
            note="Added application URLs",
        )

        with patch("tracker.emit") as mock_emit:
            ret = tracker.commandEdit(args)
            self.assertEqual(ret, tracker.EXIT_OK)
            self.assertTrue(mock_emit.called)
            emitted = mock_emit.call_args[0][0]
            self.assertTrue(emitted["dryRun"])
            # Verify trackers were stripped
            self.assertEqual(
                emitted["application"]["jobUrl"],
                "https://acme.com/jobs/123?gh_jid=999",
            )
            self.assertEqual(
                emitted["application"]["jobApplicationUrl"],
                "https://acme.com/apply?valid=true",
            )
            self.assertEqual(emitted["application"]["schemaVersion"], 7)

    @patch("tracker.client.getDb")
    def test_find_by_uuid(self, mock_get_db):
        mock_snap_by_id = MagicMock()
        mock_snap_by_id.exists = False

        mock_snap_by_uuid = MagicMock()
        mock_snap_by_uuid.to_dict.return_value = dict(self.sample_app)
        mock_snap_by_uuid.reference = "uuid-ref"

        mock_db = MagicMock()
        mock_db.collection.return_value.document.return_value.get.return_value = mock_snap_by_id
        mock_db.collection.return_value.where.return_value.limit.return_value.stream.return_value = [
            mock_snap_by_uuid
        ]

        doc, ref = tracker.findApplication(mock_db, "0192e1b4-7b00-7000-8000-000000000001")
        self.assertIsNotNone(doc)
        self.assertEqual(doc["id"], "acme--staff-android-engineer")
        self.assertEqual(ref, "uuid-ref")

    @patch("tracker.client.getDb")
    def test_edit_quarantines_unexpected_properties(self, mock_get_db):
        mock_doc_ref = MagicMock()
        mock_snap = MagicMock()
        mock_snap.exists = True
        app_with_unexpected = dict(self.sample_app)
        app_with_unexpected["statusNote"] = "stray recruiter note"
        app_with_unexpected["someRogueField"] = 12345
        mock_snap.to_dict.return_value = app_with_unexpected
        mock_doc_ref.get.return_value = mock_snap

        mock_db = MagicMock()
        mock_db.collection.return_value.document.return_value = mock_doc_ref
        mock_get_db.return_value = mock_db

        args = MagicMock(
            app_id="acme--staff-android-engineer",
            id=None,
            interactive=False,
            dry_run=True,
            company=None,
            role="Lead Android Engineer",
            status=None,
            date=None,
            channel=None,
            contact_person=None,
            contact_email=None,
            job_url=None,
            job_application_url=None,
            fit_rating=None,
            location=None,
            sector=None,
            role_type=None,
            source=None,
            note="Role update",
        )

        with patch("tracker.emit") as mock_emit:
            ret = tracker.commandEdit(args)
            self.assertEqual(ret, tracker.EXIT_OK)
            emitted = mock_emit.call_args[0][0]
            saved_app = emitted["application"]
            self.assertNotIn("statusNote", saved_app)
            self.assertNotIn("someRogueField", saved_app)
            self.assertEqual(saved_app["metadata"]["statusNote"], "stray recruiter note")
            self.assertEqual(saved_app["metadata"]["someRogueField"], 12345)
            self.assertEqual(saved_app["role"], "Lead Android Engineer")

    def test_interactive_edit_returns_only_known_fields(self):
        existing_with_extra = dict(self.sample_app)
        existing_with_extra["statusNote"] = "do not leak"
        with patch("tracker._read_line", side_effect=["13"]):  # 13 is [Save and Exit]
            updates = tracker.interactiveEdit(existing_with_extra)
            self.assertIsNotNone(updates)
            self.assertNotIn("statusNote", updates)
            self.assertNotIn("uuid", updates)
            self.assertIn("company", updates)
            self.assertIn("role", updates)


if __name__ == "__main__":
    unittest.main()
