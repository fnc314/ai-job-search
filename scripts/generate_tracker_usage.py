#!/usr/bin/env python3
"""Generate tracker.usage.kdl from application.proto.

Parses JobApplication fields in records/proto/src/main/proto/tracker/v1/application.proto
and updates or generates the `cmd "edit"` command definition in scripts/specs/tracker.usage.kdl.
"""

from __future__ import annotations

import pathlib
import re
import sys

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
PROTO_PATH = REPO_ROOT / "records" / "proto" / "src" / "main" / "proto" / "tracker" / "v1" / "application.proto"
USAGE_SPEC_PATH = REPO_ROOT / "scripts" / "specs" / "tracker.usage.kdl"


def camel_to_kebab(name: str) -> str:
    """Convert camelCase name to kebab-case."""
    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1-\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1-\2", s1).lower()


def parse_proto_fields(proto_path: pathlib.Path) -> list[tuple[str, str, str]]:
    """Extract (field_name, field_type, field_number) from JobApplication proto."""
    content = proto_path.read_text(encoding="utf-8")
    match = re.search(r"message\s+JobApplication\s*\{([^}]+)\}", content, re.DOTALL)
    if not match:
        raise ValueError(f"JobApplication message not found in {proto_path}")

    body = match.group(1)
    fields = []
    # Pattern: [repeated] Type name = number;
    pattern = re.compile(r"^\s*(repeated\s+)?([\w\.]+)\s+(\w+)\s*=\s*(\d+)\s*;", re.MULTILINE)
    for m in pattern.finditer(body):
        _repeated = bool(m.group(1))
        ftype = m.group(2)
        fname = m.group(3)
        fields.append((fname, ftype, m.group(4)))
    return fields


def build_edit_cmd_kdl(fields: list[tuple[str, str, str]]) -> str:
    """Build the Usage KDL definition for `cmd "edit"`."""
    lines = [
        'cmd "edit" help="Modify fields of an existing application record" {',
        '  arg "[id]" help="Application ID or UUID to edit"',
        '  flag "--app-id <id>" help="Application ID or UUID to edit"',
        '  flag "-i --interactive" help="Interactively select record and edit fields"',
        '  flag "--dry-run" help="Preview changes without writing"',
        '  flag "--date <date>" help="Application date (YYYY-MM-DD)"',
        '  flag "--note <note>" help="Audit note explaining the change"',
        '  flag "--contact-person <name>" help="Primary contact person name"',
        '  flag "--contact-email <email>" help="Contact email address"',
        '  flag "--fit-rating <fit>" help="Fit rating integer (0-100)"',
    ]

    # Add flags derived from proto fields (excluding immutable id/uuid from edits)
    skip_fields = {"id", "uuid", "notesRendered", "createdAt", "updatedAt", "counters", "timeline", "metadata"}
    for fname, ftype, _fnum in fields:
        if fname in skip_fields:
            continue
        flag_name = camel_to_kebab(fname)
        # Avoid duplicate definitions for fields added manually above
        if flag_name in ("date", "note", "contact-person", "contact-email", "fit-rating"):
            continue
        
        help_text = f"Update {fname}"
        if "Url" in fname:
            help_text = f"URL for {fname} (trackers will be stripped)"
        elif fname == "company":
            help_text = "Company name (vital field)"
        elif fname == "role":
            help_text = "Role title"
        elif fname == "status":
            help_text = "Application status (e.g. status:applied)"
        elif fname == "channel":
            help_text = "Application channel (e.g. channel:linkedin)"

        lines.append(f'  flag "--{flag_name} <val>" help="{help_text}"')

    lines.append("}")
    return "\n".join(lines)


def update_usage_spec():
    fields = parse_proto_fields(PROTO_PATH)
    edit_cmd_kdl = build_edit_cmd_kdl(fields)

    content = USAGE_SPEC_PATH.read_text(encoding="utf-8")
    
    # Check if cmd "edit" already exists in tracker.usage.kdl
    if 'cmd "edit"' in content:
        # Replace existing cmd "edit" block
        pattern = re.compile(r'cmd "edit" help="[^"]*"\s*\{[^}]*\}', re.DOTALL)
        content = pattern.sub(edit_cmd_kdl, content)
    else:
        # Append before any closing or at the end
        content = content.rstrip() + "\n\n" + edit_cmd_kdl + "\n"

    USAGE_SPEC_PATH.write_text(content, encoding="utf-8")
    print(f"Updated {USAGE_SPEC_PATH} with {len(fields)} fields from {PROTO_PATH}")


if __name__ == "__main__":
    update_usage_spec()
