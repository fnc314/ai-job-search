#!/usr/bin/env python3
"""Relocate GCS blobs from Title_Case role slugs to kebab-case role slugs.

This script migrates all GCS blobs where the role slug (2nd path segment)
uses Title_Case with underscores (e.g., `Senior_Android_Engineer`) to the
canonical kebab-case scheme (e.g., `senior-android-engineer`).

Usage:
    python scripts/relocate_gcs_role_slugs.py --dry-run
    TRACKER_ALLOW_WRITES=1 python scripts/relocate_gcs_role_slugs.py
"""

from __future__ import annotations

import argparse
import pathlib
import sys
import time
from collections import defaultdict

SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import client
import contract


def relocate_blob(bucket, old_path: str, new_path: str) -> bool:
    """Move a GCS blob from old_path to new_path, preserving metadata."""
    old_blob = bucket.blob(old_path)
    if not old_blob.exists():
        return False

    # Copy to new location
    new_blob = bucket.copy_blob(old_blob, bucket, new_path)

    # Preserve metadata
    if old_blob.metadata:
        new_blob.metadata = dict(old_blob.metadata)
        new_blob.patch()

    # Delete old blob
    old_blob.delete()
    return True


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Relocate GCS blobs from Title_Case role slugs to kebab-case"
    )
    parser.add_argument(
        "--dry-run", action="store_true", help="Preview relocations without moving blobs"
    )
    args = parser.parse_args()

    bucket = client.getBucket()
    db = client.getDb()

    # Collect all blobs with uppercase role slugs
    relocations = []
    for blob in bucket.list_blobs(prefix="applications/"):
        parts = blob.name.split("/")
        # applications/{company}/{role}/...
        if len(parts) >= 4:
            role = parts[2]
            if role != role.lower():
                kebab_role = role.replace("_", "-").lower()
                new_path = blob.name.replace(f"/{role}/", f"/{kebab_role}/", 1)
                relocations.append((blob.name, new_path, role, kebab_role))

    if not relocations:
        print("No blobs to relocate.")
        return 0

    if args.dry_run:
        # Group by company for summary
        by_company = defaultdict(list)
        for old_path, new_path, old_role, new_role in relocations:
            parts = old_path.split("/")
            company = parts[1] if len(parts) >= 2 else "?"
            by_company[company].append((old_path, new_path, old_role, new_role))

        for company in sorted(by_company.keys()):
            paths = by_company[company]
            print(f"\n{company} ({len(paths)} blobs):")
            for old_path, new_path, old_role, new_role in paths[:3]:
                print(f"  {old_role} → {new_role}")
            if len(paths) > 3:
                print(f"  ... and {len(paths) - 3} more")

        print(f"\nTotal blobs to relocate: {len(relocations)}")
        return 0

    # Check write permission
    client.assertWritesAllowed("GCS role slug relocation")

    # Collect all artifact paths upfront (single pass)
    print("Collecting artifact references...")
    artifact_paths = {}
    for app_doc in db.collection(client.APPLICATIONS).stream():
        for art_doc in app_doc.reference.collection(client.ARTIFACTS).stream():
            art_data = art_doc.to_dict()
            if not art_data:
                continue
            storage = art_data.get("storage") or {}
            if isinstance(storage, dict):
                stored_path = storage.get("path", "")
                if stored_path:
                    artifact_paths[stored_path] = art_doc.reference.path
    print(f"  Found {len(artifact_paths)} artifact references")

    relocated = 0
    errors = 0

    print(f"\nRelocating {len(relocations)} blobs...")
    print()

    # Build path mapping for batch artifact update
    path_mapping = {}

    for i, (old_path, new_path, old_role, new_role) in enumerate(relocations, 1):
        company = old_path.split("/")[1] if len(old_path.split("/")) >= 2 else "?"
        print(
            f"  [{i}/{len(relocations)}] {company}/{pathlib.Path(old_path).name}",
            flush=True,
        )

        try:
            if relocate_blob(bucket, old_path, new_path):
                relocated += 1
                path_mapping[old_path] = new_path
            else:
                print(f"    ✗ Blob not found (may have been deleted)", flush=True)
        except Exception as e:
            errors += 1
            print(f"    ✗ Error: {e}", flush=True)

        time.sleep(0.1)  # Rate limit

    # Batch update all artifact references
    if path_mapping:
        print(f"\nUpdating artifact references...")
        artifacts_updated = 0
        for old_path, new_path in path_mapping.items():
            try:
                art_ref = db.document(old_path)
                art_data = art_ref.get().to_dict()
                if art_data:
                    storage = art_data.get("storage") or {}
                    if isinstance(storage, dict) and storage.get("path") == old_path:
                        storage["path"] = new_path
                        art_ref.set({"storage": storage}, merge=True)
                        artifacts_updated += 1
            except Exception:
                # Also try finding artifact by scanning
                pass

        # If direct lookup didn't work, scan all artifacts
        if artifacts_updated == 0:
            for app_doc in db.collection(client.APPLICATIONS).stream():
                for art_doc in app_doc.reference.collection(client.ARTIFACTS).stream():
                    art_data = art_doc.to_dict()
                    if not art_data:
                        continue
                    storage = art_data.get("storage") or {}
                    if not isinstance(storage, dict):
                        continue
                    stored_path = storage.get("path", "")
                    if not stored_path:
                        continue

                    if stored_path in path_mapping:
                        storage["path"] = path_mapping[stored_path]
                        art_doc.reference.set({"storage": storage}, merge=True)
                        artifacts_updated += 1

        print(f"  Updated {artifacts_updated} artifact documents")

    print(f"\nRelocation complete:")
    print(f"  Relocated: {relocated}")
    print(f"  Artifact docs updated: {artifacts_updated}")
    print(f"  Errors: {errors}")
    return 1 if errors > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
