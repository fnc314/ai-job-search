import pathlib
import sys
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))
from urls import clean_url


class TestCleanUrl(unittest.TestCase):
    def test_strip_utm_parameters(self):
        url = "https://example.com/job?utm_source=linkedin&utm_medium=cpc&utm_campaign=hiring"
        self.assertEqual(clean_url(url), "https://example.com/job")

    def test_strip_tracking_params_preserves_functional_params(self):
        # gh_jid is functional, gh_src is tracking
        url = "https://www.coinbase.com/careers/positions/8241716?gh_jid=8241716&gh_src=r4rlpx331us"
        self.assertEqual(
            clean_url(url),
            "https://www.coinbase.com/careers/positions/8241716?gh_jid=8241716",
        )

    def test_strip_click_ids(self):
        url = "https://careers.google.com/jobs/results/12345?fbclid=abc123xyz&gclid=def456"
        self.assertEqual(
            clean_url(url),
            "https://careers.google.com/jobs/results/12345",
        )

    def test_preserves_hash_fragments(self):
        url = "https://boards.greenhouse.io/company/jobs/12345?utm_source=jobboard#application"
        self.assertEqual(
            clean_url(url),
            "https://boards.greenhouse.io/company/jobs/12345#application",
        )

    def test_empty_or_none(self):
        self.assertIsNone(clean_url(None))
        self.assertEqual(clean_url(""), "")
        self.assertEqual(clean_url("   "), "")

    def test_clean_url_untouched_if_no_trackers(self):
        url = "https://jobs.lever.co/company/abc-123"
        self.assertEqual(clean_url(url), url)
