"""Tests for logo fetching and metadata extraction."""

from __future__ import annotations

import io
import os
import pathlib
import struct
import sys
import unittest
from unittest.mock import MagicMock, patch

ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
if str(ROOT / "scripts") not in sys.path:
    sys.path.insert(0, str(ROOT / "scripts"))

import client
import tracker


class TestDomainNormalization(unittest.TestCase):
    def test_clean_urls(self):
        self.assertEqual(tracker.normalizeDomain("https://www.affirm.com/careers"), "affirm.com")
        self.assertEqual(tracker.normalizeDomain("http://ebay.com:8080/path?param=1"), "ebay.com")
        self.assertEqual(tracker.normalizeDomain("https://stripe.com/"), "stripe.com")

    def test_aliases(self):
        self.assertEqual(tracker.normalizeDomain("general-motors"), "gm.com")
        self.assertEqual(tracker.normalizeDomain("ford-motor-company"), "ford.com")
        self.assertEqual(tracker.normalizeDomain("american-eagle-outfitters"), "aeo.com")
        self.assertEqual(tracker.normalizeDomain("major-league-baseball"), "mlb.com")
        self.assertEqual(tracker.normalizeDomain("carnegie-mellon-university"), "cmu.edu")
        self.assertEqual(tracker.normalizeDomain("block"), "block.xyz")

    def test_generic_ats_urls_ignored(self):
        self.assertEqual(tracker.normalizeDomain("https://www.linkedin.com/jobs/view/123"), "")
        self.assertEqual(tracker.normalizeDomain("https://jobs.lever.co/company/456"), "")
        self.assertEqual(tracker.normalizeDomain("https://boards.greenhouse.io/corp"), "")
        self.assertEqual(tracker.normalizeDomain("https://capitalone.wd12.myworkdayjobs.com/jobs"), "")

    def test_slug_fallback(self):
        self.assertEqual(tracker.normalizeDomain("capital-one"), "capitalone.com")
        self.assertEqual(tracker.normalizeDomain("butterfly-network"), "butterflynetwork.com")
        self.assertEqual(tracker.normalizeDomain("duolingo"), "duolingo.com")


class TestBinaryMetadataExtraction(unittest.TestCase):
    def test_png_extraction(self):
        # 8-byte signature + 4-byte len + 4-byte IHDR + 4-byte width + 4-byte height
        png_data = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR" + struct.pack(">II", 500, 400) + b"\x08\x06\x00\x00\x00"
        fmt, w, h, shape = tracker.extractImageMetadata(png_data, "image/png")
        self.assertEqual(fmt, "PNG")
        self.assertEqual(w, 500)
        self.assertEqual(h, 400)
        self.assertEqual(shape, "RECTANGLE")

    def test_png_square(self):
        png_data = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR" + struct.pack(">II", 256, 256) + b"\x08\x06\x00\x00\x00"
        fmt, w, h, shape = tracker.extractImageMetadata(png_data, "image/png")
        self.assertEqual(fmt, "PNG")
        self.assertEqual(w, 256)
        self.assertEqual(h, 256)
        self.assertEqual(shape, "SQUARE")

    def test_avif_ispe_extraction(self):
        ftyp = b"\x00\x00\x00\x1cftypavif\x00\x00\x00\x00avifmif1"
        ispe = b"\x00\x00\x00\x14ispe\x00\x00\x00\x00" + struct.pack(">II", 1024, 537)
        avif_data = ftyp + ispe
        fmt, w, h, shape = tracker.extractImageMetadata(avif_data, "image/avif")
        self.assertEqual(fmt, "AVIF")
        self.assertEqual(w, 1024)
        self.assertEqual(h, 537)
        self.assertEqual(shape, "RECTANGLE")

    def test_webp_vp8x_extraction(self):
        # RIFF ... WEBP VP8X ...
        header = b"RIFF\x00\x00\x00\x00WEBPVP8X\x0a\x00\x00\x00\x00\x00\x00\x00"
        # 24-bit width (300-1 = 299) and height (200-1 = 199)
        dims = (299).to_bytes(3, "little") + (199).to_bytes(3, "little")
        webp_data = header + dims
        fmt, w, h, shape = tracker.extractImageMetadata(webp_data, "image/webp")
        self.assertEqual(fmt, "WEBP")
        self.assertEqual(w, 300)
        self.assertEqual(h, 200)
        self.assertEqual(shape, "RECTANGLE")

    def test_svg_detection(self):
        svg_data = b'<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"></svg>'
        fmt, w, h, shape = tracker.extractImageMetadata(svg_data, "image/svg+xml")
        self.assertEqual(fmt, "SVG")
        self.assertEqual(shape, "SQUARE")


class TestLogoFetchCommand(unittest.TestCase):
    @patch.dict(os.environ, {}, clear=True)
    def test_write_permission_required_when_not_dry_run(self):
        parser = tracker.buildParser()
        args = parser.parse_args(["logo-fetch", "--company", "affirm"])
        with self.assertRaises(client.WriteNotAllowed):
            tracker.commandLogoFetch(args)

    @patch("tracker.client.getDb")
    @patch("tracker.fetchLogo")
    def test_dry_run_performs_no_writes(self, mock_fetch, mock_get_db):
        mock_db = MagicMock()
        mock_get_db.return_value = mock_db
        mock_fetch.return_value = {
            "logoUrl": "https://logos.hunter.io/affirm.com",
            "logoFormat": "AVIF",
            "logoWidth": 1024,
            "logoHeight": 537,
            "logoShape": "RECTANGLE",
            "logoFetchedAt": "2026-11-08T18:00:00Z",
        }
        mock_doc = MagicMock()
        mock_doc.exists = False
        mock_db.collection().document().get.return_value = mock_doc

        parser = tracker.buildParser()
        args = parser.parse_args(["logo-fetch", "--company", "affirm", "--dry-run"])

        with patch("sys.stdout", new=io.StringIO()):
            exit_code = tracker.commandLogoFetch(args)

        self.assertEqual(exit_code, 0)
        # Verify db.collection().document().set was NOT called
        mock_db.collection().document().set.assert_not_called()


if __name__ == "__main__":
    unittest.main()
