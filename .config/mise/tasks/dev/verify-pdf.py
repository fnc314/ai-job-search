#!/usr/bin/env python3
#MISE description="Verify that a generated PDF has the expected pages and extractable text"
#MISE alias="verify-pdf"
#USAGE arg "<pdf>" help="Path to PDF file"
#USAGE flag "-p --pages <count>" help="Expected number of pages"
#USAGE flag "-m --min-chars <count>" help="Minimum characters expected"
#USAGE flag "-c --contains <text>" help="Substring that must be present in extractable text"

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()


class VerificationError(Exception):
    """Raised when a generated PDF does not satisfy its checks."""


def run_tool(command):
    try:
        return subprocess.run(
            command,
            check=True,
            capture_output=True,
            text=True,
        ).stdout
    except FileNotFoundError as exc:
        raise VerificationError(
            f"required command '{command[0]}' was not found; install poppler-utils"
        ) from exc
    except subprocess.CalledProcessError as exc:
        detail = (exc.stderr or "").strip() or (exc.stdout or "").strip()
        detail = detail or "command failed"
        raise VerificationError(f"{command[0]} could not read the PDF: {detail}") from exc


def parse_page_count(pdfinfo_output):
    match = re.search(r"^Pages:\s+(\d+)\s*$", pdfinfo_output, re.MULTILINE)
    if not match:
        raise VerificationError("pdfinfo output did not contain a page count")
    return int(match.group(1))


def normalize_text(text):
    return " ".join(text.split())


def verify_pdf(pdf_path, expected_pages=None, min_chars=1, required_text=()):
    pdf_path = Path(pdf_path)
    if not pdf_path.is_file():
        raise VerificationError(f"PDF does not exist: {pdf_path}")

    if expected_pages is not None:
        actual_pages = parse_page_count(run_tool(["pdfinfo", str(pdf_path)]))
        if actual_pages != expected_pages:
            raise VerificationError(
                f"expected {expected_pages} page(s), but found {actual_pages}"
            )

    text = run_tool(["pdftotext", str(pdf_path), "-"])
    char_count = len(text.strip())
    if char_count < min_chars:
        raise VerificationError(
            f"expected at least {min_chars} character(s) of extractable text, "
            f"but found {char_count}"
        )

    normalized_body = normalize_text(text)
    for needle in required_text:
        normalized_needle = normalize_text(needle)
        if normalized_needle not in normalized_body:
            raise VerificationError(
                f"missing required text: {needle!r}"
            )


def build_parser():
    parser = argparse.ArgumentParser(
        description="Verify page count and extractable text of a generated PDF."
    )
    parser.add_argument("pdf", help="path to the PDF file to inspect")
    parser.add_argument(
        "--pages",
        type=int,
        default=None,
        help="expected page count",
    )
    parser.add_argument(
        "--min-chars",
        type=int,
        default=1,
        help="minimum number of extractable characters (default: 1)",
    )
    parser.add_argument(
        "--contains",
        action="append",
        default=[],
        help="substring that must be present in extractable text (can repeat)",
    )
    return parser


def main():
    args = build_parser().parse_args()
    try:
        verify_pdf(
            pdf_path=args.pdf,
            expected_pages=args.pages,
            min_chars=args.min_chars,
            required_text=args.contains,
        )
    except VerificationError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
