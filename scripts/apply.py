#!/usr/bin/env python3
"""DEPRECATED: Application generation orchestrator — deferred, manages CV + cover letter generation.

apply.py — CLI entry point for the apply workflow.

With a URL/JD argument: checks the tracker for existing records and prints
guidance to run the /apply Claude command for the full agent-driven workflow.

Without arguments (interactive mode): manages DRAFT applications (status
DRAFTING) — review, mark as submitted, or discard.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent


def _tracker(*args: str) -> subprocess.CompletedProcess[str]:
    """Run the tracker CLI and return the result."""
    cmd = [
        sys.executable,
        str(REPO_ROOT / "scripts" / "tracker.py"),
    ] + list(args)
    return subprocess.run(cmd, capture_output=True, text=True, timeout=30)


def _list_applications(status: str | None = None) -> list[dict]:
    """Query tracker list and return the documents array."""
    args = ["list", "--json"]
    if status:
        args.extend(["--status", status])
    result = _tracker(*args)
    if result.returncode != 0:
        print(f"Error querying tracker: {result.stderr.strip()}", file=sys.stderr)
        return []
    try:
        data = json.loads(result.stdout)
        return data.get("documents", [])
    except (json.JSONDecodeError, KeyError):
        return []


def _slugify(s: str) -> str:
    """Convert a string to a kebab-case slug for matching."""
    import re
    s = re.sub(r"[^a-z0-9]+", "-", s.lower().strip())
    return s.strip("-")


def _find_existing(company: str, role: str) -> dict | None:
    """Check if an application for company+role already exists."""
    apps = _list_applications()
    target = f"{_slugify(company)}-{_slugify(role)}"
    for app in apps:
        app_company = app.get("company", "") or ""
        app_role = app.get("role", "") or app.get("roleType", "") or ""
        app_key = f"{_slugify(app_company)}-{_slugify(app_role)}"
        if app_key == target:
            return app
    return None


def handle_url_or_jd(url_or_jd: str) -> int:
    """Handle the case when a URL or JD is provided as argument."""
    # Try to extract company and role from the input for tracker lookup
    # If it's a URL, we can't easily parse company/role without fetching,
    # so we just provide guidance.
    is_url = url_or_jd.startswith("http")

    if is_url:
        print(f"Job URL provided: {url_or_jd}")
        print()
        print("To apply to this position, run the /apply Claude command:")
        print(f"  /apply {url_or_jd}")
        print()
        print("The /apply workflow will:")
        print("  1. Evaluate fit against your profile")
        print("  2. Draft a tailored CV and cover letter")
        print("  3. Run a reviewer agent for quality checks")
        print("  4. Compile PDFs and present the final artifacts")
    else:
        # Text-based JD — try to extract company/role for tracker check
        lines = url_or_jd.strip().split("\n")
        first_line = lines[0].strip() if lines else ""

        # Heuristic: try to find company and role in the first few lines
        company = ""
        role = ""
        for line in lines[:5]:
            line_stripped = line.strip()
            if not line_stripped:
                continue
            # Look for patterns like "Company - Role" or "Role at Company"
            if " at " in line_stripped:
                parts = line_stripped.split(" at ", 1)
                role = parts[0].strip()
                company = parts[1].strip()
                break
            elif " - " in line_stripped:
                parts = line_stripped.split(" - ", 1)
                company = parts[0].strip()
                role = parts[1].strip()
                break

        if company and role:
            existing = _find_existing(company, role)
            if existing:
                app_id = existing.get("id", "unknown")
                app_status = existing.get("status", "unknown")
                print(f"Found existing record: {app_id}")
                print(f"  Company: {company}")
                print(f"  Role: {role}")
                print(f"  Status: {app_status}")
                print()
                if app_status and "DRAFTING" in app_status.upper():
                    print("This application is in DRAFTING status.")
                    print("Run /apply to continue the agent-driven workflow,")
                    print("or run 'mise apply' (no args) to manage drafts interactively.")
                else:
                    print(f"This application is already in {app_status} status.")
                    print("If you need to re-apply, run /apply with the job details.")
            else:
                print(f"New application: {company} — {role}")
                print()
                print("To proceed, run the /apply Claude command:")
                print("  /apply <paste job description>")
                print()
                print("The /apply workflow will evaluate fit, draft artifacts,")
                print("and present the final application package.")
        else:
            print("Job description text provided.")
            print()
            print("To apply, run the /apply Claude command with the full")
            print("job description. The agent will evaluate fit and draft")
            print("tailored application artifacts.")

    return 0


def interactive_mode() -> int:
    """Interactive mode: manage DRAFT applications."""
    drafts = _list_applications(status="DRAFTING")

    if not drafts:
        print("No draft applications found.")
        print()
        print("To start a new application, run:")
        print("  /apply <url-or-job-description>")
        return 0

    print(f"Found {len(drafts)} draft application(s):\n")
    for i, draft in enumerate(drafts, 1):
        app_id = draft.get("id", "unknown")
        company = draft.get("company", "Unknown")
        role = draft.get("role", draft.get("roleType", "Unknown"))
        date = draft.get("date", "unknown")
        status = draft.get("status", "unknown")
        print(f"  {i}. {company} — {role}")
        print(f"     ID: {app_id} | Status: {status} | Date: {date}")

    print()

    try:
        choice = input("Select a draft to manage (number, or 'q' to quit): ").strip()
    except (EOFError, KeyboardInterrupt):
        print()
        return 0

    if choice.lower() == "q":
        return 0

    try:
        idx = int(choice) - 1
        if idx < 0 or idx >= len(drafts):
            print("Invalid selection.")
            return 1
    except ValueError:
        print("Invalid selection.")
        return 1

    draft = drafts[idx]
    app_id = draft.get("id", "unknown")
    company = draft.get("company", "Unknown")
    role = draft.get("role", draft.get("roleType", "Unknown"))
    date = draft.get("date", "unknown")
    status = draft.get("status", "unknown")

    print(f"\nDraft: {company} — {role}")
    print(f"  ID: {app_id}")
    print(f"  Status: {status}")
    print(f"  Date: {date}")
    print()

    # Fetch artifacts for this draft
    result = _tracker("get", app_id)
    artifacts = []
    if result.returncode == 0:
        try:
            data = json.loads(result.stdout)
            artifacts = data.get("artifacts", [])
        except (json.JSONDecodeError, KeyError):
            pass

    if artifacts:
        print(f"  Artifacts ({len(artifacts)}):")
        for art in artifacts:
            art_type = art.get("type", "unknown")
            art_path = art.get("path", art.get("uri", "unknown"))
            print(f"    - {art_type}: {art_path}")
        print()

    print("Options:")
    print("  1. Mark as submitted (transition to APPLIED)")
    print("  2. Re-open /apply in Claude for editing")
    print("  3. Discard this draft")
    print()

    try:
        action = input("Select an action (1-3, or 'q' to quit): ").strip()
    except (EOFError, KeyboardInterrupt):
        print()
        return 0

    if action.lower() == "q":
        return 0

    if action == "1":
        # Mark as submitted: transition DRAFTING → APPLIED
        print(f"Transitioning {app_id} to APPLIED...")
        result = _tracker("status", app_id, "APPLIED")
        if result.returncode == 0:
            print(f"Successfully marked as submitted.")
            print(f"  {company} — {role} is now in APPLIED status.")
        else:
            print(f"Error: {result.stderr.strip()}", file=sys.stderr)
            return 1

    elif action == "2":
        print(f"To continue editing this draft, run:")
        print(f"  /apply")
        print(f"and reference the application ID: {app_id}")

    elif action == "3":
        print(f"To discard this draft, you can transition it to APPLICATION_WITHDRAWN:")
        print(f"  mise run tracker status {app_id} APPLICATION_WITHDRAWN")

    else:
        print("Invalid action.")
        return 1

    return 0


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Manage job applications — check status or manage drafts"
    )
    parser.add_argument(
        "url_or_jd",
        nargs="?",
        default=None,
        help="Job URL or job description text",
    )
    args = parser.parse_args()

    if args.url_or_jd:
        return handle_url_or_jd(args.url_or_jd)
    else:
        return interactive_mode()


if __name__ == "__main__":
    sys.exit(main())
