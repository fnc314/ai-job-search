#!/usr/bin/env python3
#MISE description="Modify fields of an existing application record"
#USAGE arg "[id]" help="Application ID or UUID to edit"
#USAGE flag "--app-id <app_id>" help="Application ID or UUID to edit"
#USAGE flag "-i --interactive" help="Interactively select record and edit fields"
#USAGE flag "--dry-run" help="Preview changes without writing"
#USAGE flag "--job-url <job_url>" help="Job posting URL"
#USAGE flag "--job-application-url <job_application_url>" help="Job application portal URL"
#USAGE flag "--company <company>" help="Company name"
#USAGE flag "--role <role>" help="Role title"
#USAGE flag "--status <status>" help="Target status"
#USAGE flag "--date <date>" help="Application date"
#USAGE flag "--contact-person <contact_person>" help="Contact person"
#USAGE flag "--contact-email <contact_email>" help="Contact email"
#USAGE flag "--fit-rating <fit_rating>" help="Fit rating"
#USAGE flag "--note <note>" help="Audit note"

import os
import sys
from pathlib import Path

import _guard
_guard.enforce_mise()

def main():
    root = Path(__file__).resolve()
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    tracker_script = root / "scripts" / "tracker.py"
    import runpy
    sys.argv = [str(tracker_script), "edit"] + sys.argv[1:]
    runpy.run_path(str(tracker_script), run_name="__main__")

if __name__ == "__main__":
    main()
