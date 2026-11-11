#!/usr/bin/env python3
"""ARCHIVED: One-time GCS cleanup, already completed. No Kotlin migration needed.

Purge legacy GCS artifacts and migrate orphans to the canonical path structure.

The GCS bucket contains 62 objects using the old flat path format
`applications/{id}/{kind}/{filename}`. This script handles them in three ways:

1. **Safe delete** (40 objects): Old-style objects that have a new-style
   counterpart with the same filename. Just delete the old one.

2. **Local re-upload** (4 objects): Orphaned old-style objects whose source
   file exists locally in `job-applications/`. Re-upload to the new
   canonical path, then delete the old one.

3. **GCS-to-GCS migrate** (18 objects): Orphaned old-style objects that only
   exist in GCS. Download from the old path, re-upload to the new canonical
   path, then delete the old one.

Usage:
    python scripts/purge_gcs_legacy.py --dry-run          # Preview what will happen
    TRACKER_ALLOW_WRITES=1 python scripts/purge_gcs_legacy.py   # Execute purge
"""

from __future__ import annotations

import argparse
import os
import pathlib
import re
import sys
import tempfile

SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import client
import contract
import artifacts as artifactsModule


def classify_blobs(bucket) -> dict:
    """Scan the GCS bucket and classify objects into old-style and new-style.

    Returns a dict with keys: safe_delete, orphan_local, orphan_gcs_only.
    """
    blobs = list(bucket.list_blobs(prefix="applications/"))

    old_style = []
    new_style = []

    for b in blobs:
        parts = b.name.split("/")
        if len(parts) == 4 and parts[0] == "applications":
            old_style.append(b)
        elif len(parts) == 5 and parts[0] == "applications" and re.match(
            r"^\d{4}-\d{2}-\d{2}$", parts[3]
        ):
            new_style.append(b)

    # Build index of new-style filenames (lowercase)
    new_filenames = set()
    for b in new_style:
        parts = b.name.split("/")
        new_filenames.add(parts[4].lower())

    # Separate old-style into: has new copy vs orphan
    has_new_copy = []
    orphans = []

    for b in old_style:
        parts = b.name.split("/")
        filename = parts[3].lower()
        if filename in new_filenames:
            has_new_copy.append(b)
        else:
            orphans.append(b)

    # Check orphans against local files
    docs_dir = pathlib.Path("job-applications")
    orphan_local = []
    orphan_gcs_only = []

    for b in orphans:
        parts = b.name.split("/")
        filename = parts[3]
        matches = list(docs_dir.rglob(filename)) if docs_dir.exists() else []
        if matches:
            orphan_local.append((b, str(matches[0])))
        else:
            orphan_gcs_only.append(b)

    return {
        "safe_delete": has_new_copy,
        "orphan_local": orphan_local,
        "orphan_gcs_only": orphan_gcs_only,
    }


def parse_old_id(old_id: str) -> tuple[str, str]:
    """Parse old kebab-case ID into (company, role).

    Old format: company--role (e.g., 'anthropic--staff-software-engineer-android')
    """
    parts = old_id.split("--", 1)
    if len(parts) >= 2:
        company = parts[0].replace("-", " ")
        role = parts[1].replace("-", " ")
        return company, role
    return old_id.replace("-", " "), ""


def infer_kind_from_filename(filename: str, old_kind: str) -> str:
    """Infer the artifact kind from the old kind folder and filename."""
    # Map old kind folder names to artifact kinds
    kind_map = {
        "cv": "artifact:cv",
        "cover-letter": "artifact:cover-letter",
    }
    return kind_map.get(old_kind, "artifact:cv")


def infer_date_for_orphan(bucket, old_blob) -> str:
    """Try to infer a date for an orphaned file by checking Firestore for
    a matching application, or fall back to the blob's creation date."""
    parts = old_blob.name.split("/")
    old_id = parts[1]
    company, role = parse_old_id(old_id)

    # Try to find a matching application in Firestore
    try:
        db = client.getDb()
        for app_doc in db.collection(client.APPLICATIONS).stream():
            app_data = app_doc.to_dict()
            app_company = (app_data.get("company") or "").lower()
            app_role = (app_data.get("role") or "").lower()
            comp_lower = contract.normalize_dir_name(company).lower()
            role_lower = contract.normalize_dir_name(role).lower()
            if app_company == comp_lower or app_company == company.lower():
                if app_role == role_lower or app_role == role.lower():
                    date = (app_data.get("date") or "")[:10]
                    if date:
                        return date
    except Exception:
        pass

    # Fall back to blob creation date
    created = old_blob.time_created
    if created:
        return created.strftime("%Y-%m-%d")
    return "2026-01-01"


def do_purge(bucket, classifications: dict, dry_run: bool):
    """Execute the purge based on classifications."""
    total = 0

    # 1. Safe deletes
    print(f"\n{'[DRY RUN] ' if dry_run else ''}=== Safe deletes (old has new copy) ===")
    for b in classifications["safe_delete"]:
        total += 1
        print(f"  {'[DRY RUN] Would delete: ' if dry_run else 'Deleted: '}{b.name}")
        if not dry_run:
            b.delete()

    # 2. Orphans with local source
    print(f"\n{'[DRY RUN] ' if dry_run else ''}=== Orphan re-upload from local files ===")
    for b, local_path in classifications["orphan_local"]:
        total += 1
        parts = b.name.split("/")
        old_id = parts[1]
        old_kind = parts[2]
        company, role = parse_old_id(old_id)
        kind = infer_kind_from_filename(parts[3], old_kind)

        # Build new path
        new_path = artifactsModule.blobPath(
            company, role, "2026-01-01", kind, parts[3]
        )
        print(f"  {'[DRY RUN] Would re-upload: ' if dry_run else 'Re-uploaded: '}{b.name}")
        print(f"    -> {new_path}")

        if not dry_run:
            try:
                storage = artifactsModule.upload(
                    bucket,
                    company=company,
                    role=role,
                    date="2026-01-01",
                    kind=kind,
                    path=local_path,
                )
                print(f"    Uploaded to: {storage['path']}")
                b.delete()
                print(f"    Deleted old: {b.name}")
            except Exception as e:
                print(f"    Error: {e}")

    # 3. Orphans only in GCS
    print(f"\n{'[DRY RUN] ' if dry_run else ''}=== Orphan GCS-to-GCS migration ===")
    for b in classifications["orphan_gcs_only"]:
        total += 1
        parts = b.name.split("/")
        old_id = parts[1]
        old_kind = parts[2]
        company, role = parse_old_id(old_id)
        kind = infer_kind_from_filename(parts[3], old_kind)
        date = infer_date_for_orphan(bucket, b)

        new_path = artifactsModule.blobPath(
            company, role, date, kind, parts[3]
        )
        print(f"  {'[DRY RUN] Would migrate: ' if dry_run else 'Migrating: '}{b.name}")
        print(f"    -> {new_path}")

        if not dry_run:
            try:
                # Download from old path
                data = b.download_as_bytes()

                # Upload to new path using a temp file
                with tempfile.NamedTemporaryFile(
                    delete=False, suffix=parts[3]
                ) as tmp:
                    tmp.write(data)
                    tmp_path = tmp.name

                try:
                    storage = artifactsModule.upload(
                        bucket,
                        company=company,
                        role=role,
                        date=date,
                        kind=kind,
                        path=tmp_path,
                    )
                    print(f"    Uploaded to: {storage['path']}")
                    b.delete()
                    print(f"    Deleted old: {b.name}")
                finally:
                    os.unlink(tmp_path)
            except Exception as e:
                print(f"    Error: {e}")

    print(f"\n{'[DRY RUN] ' if dry_run else ''}Total: {total} objects processed")
    return total


def main():
    parser = argparse.ArgumentParser(
        description="Purge legacy GCS artifacts and migrate orphans"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview what would be done without making changes",
    )
    args = parser.parse_args()

    if not args.dry_run and os.environ.get("TRACKER_ALLOW_WRITES") != "1":
        print("Error: Set TRACKER_ALLOW_WRITES=1 to enable writes.")
        sys.exit(1)

    print("Scanning GCS bucket...")
    bucket = client.getBucket()
    classifications = classify_blobs(bucket)

    safe_count = len(classifications["safe_delete"])
    local_count = len(classifications["orphan_local"])
    gcs_count = len(classifications["orphan_gcs_only"])
    total = safe_count + local_count + gcs_count

    print(f"\nFound {total} old-style objects:")
    print(f"  Safe to delete (have new copy): {safe_count}")
    print(f"  Orphan with local source:       {local_count}")
    print(f"  Orphan only in GCS:             {gcs_count}")

    if args.dry_run:
        print("\n[Dry run — no changes will be made]")

    do_purge(bucket, classifications, args.dry_run)


if __name__ == "__main__":
    main()
