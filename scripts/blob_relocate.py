#!/usr/bin/env python3
"""Relocate GCS blobs when company slugs change due to alias updates.

When a company slug alias changes (e.g., `ford-motor` → `ford`), this script
moves all GCS blobs from the old prefix to the new prefix and updates all
Firestore artifact documents that reference the old paths.

Usage:
    python scripts/relocate_gcs_blobs.py --dry-run
    python scripts/relocate_gcs_blobs.py --old-slug ford-motor --new-slug ford
    TRACKER_ALLOW_WRITES=1 python scripts/relocate_gcs_blobs.py --old-slug ford-motor --new-slug ford
"""

from __future__ import annotations

import argparse
import pathlib
import sys
import time

SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import client
import contract


def find_blobs_with_prefix(bucket, prefix: str) -> list:
    """List all blobs under a given prefix."""
    return list(bucket.list_blobs(prefix=prefix))


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


def update_artifact_storage(db, old_path: str, new_path: str, bucket_name: str) -> int:
    """Update all artifact documents that reference the old GCS path."""
    updated = 0
    for app_doc in db.collection(client.APPLICATIONS).stream():
        for art_doc in app_doc.reference.collection(client.ARTIFACTS).stream():
            art_data = art_doc.to_dict()
            storage = art_data.get("storage") or {}
            if isinstance(storage, dict):
                stored_path = storage.get("path", "")
                stored_bucket = storage.get("bucket", "")
                if stored_path == old_path or (stored_bucket == bucket_name and stored_path == old_path):
                    storage["path"] = new_path
                    art_doc.reference.set({"storage": storage}, merge=True)
                    updated += 1
    return updated


def discover_slug_changes() -> list[tuple[str, str]]:
    """Discover company slug changes by comparing old aliases to new canonical slugs.

    Returns a list of (old_slug, new_slug) pairs where the old slug no longer
    maps to itself.
    """
    changes = []
    # Check all known aliases — any alias that normalizes to a different slug
    # is a potential migration target
    aliases = contract._COMPANY_SLUG_ALIASES
    for old_slug, new_slug in aliases.items():
        if old_slug != new_slug:
            changes.append((old_slug, new_slug))
    return changes


def main() -> int:
    parser = argparse.ArgumentParser(description="Relocate GCS blobs when company slugs change")
    parser.add_argument("--dry-run", action="store_true", help="Preview relocations without moving blobs")
    parser.add_argument("--old-slug", help="Old company slug (e.g., ford-motor)")
    parser.add_argument("--new-slug", help="New company slug (e.g., ford)")
    args = parser.parse_args()

    # Determine slug pairs to process
    if args.old_slug and args.new_slug:
        slug_pairs = [(args.old_slug, args.new_slug)]
    else:
        slug_pairs = discover_slug_changes()
        if not slug_pairs:
            print("No slug changes detected.")
            return 0

    db = client.getDb()

    if args.dry_run:
        bucket = client.getBucket()
        total_blobs = 0
        for old_slug, new_slug in slug_pairs:
            old_prefix = f"{client.APPLICATIONS}/{old_slug}/"
            new_prefix = f"{client.APPLICATIONS}/{new_slug}/"
            blobs = find_blobs_with_prefix(bucket, old_prefix)
            if blobs:
                print(f"\n{old_slug} → {new_slug}:")
                for blob in blobs:
                    new_path = blob.name.replace(old_prefix, new_prefix, 1)
                    print(f"  {blob.name} → {new_path}")
                    total_blobs += 1
        print(f"\nTotal blobs to relocate: {total_blobs}")
        return 0

    # Check write permission
    client.assertWritesAllowed("GCS blob relocation")
    bucket = client.getBucket()

    relocated = 0
    artifacts_updated = 0
    errors = 0

    for old_slug, new_slug in slug_pairs:
        old_prefix = f"{client.APPLICATIONS}/{old_slug}/"
        new_prefix = f"{client.APPLICATIONS}/{new_slug}/"
        blobs = find_blobs_with_prefix(bucket, old_prefix)

        if not blobs:
            print(f"No blobs found for {old_slug}/")
            continue

        print(f"\nRelocating {len(blobs)} blobs: {old_slug} → {new_slug}")

        for i, blob in enumerate(blobs, 1):
            new_path = blob.name.replace(old_prefix, new_prefix, 1)
            print(f"  [{i}/{len(blobs)}] {blob.name} → {new_path}", flush=True)

            try:
                if relocate_blob(bucket, blob.name, new_path):
                    relocated += 1

                    # Update Firestore artifact references
                    updated = update_artifact_storage(db, blob.name, new_path, bucket.name)
                    artifacts_updated += updated

                    if updated:
                        print(f"    ✓ Updated {updated} artifact document(s)", flush=True)
                else:
                    print(f"    ✗ Blob not found (may have been deleted)", flush=True)
            except Exception as e:
                errors += 1
                print(f"    ✗ Error: {e}", flush=True)

            time.sleep(0.1)  # Rate limit

    print(f"\nRelocation complete:")
    print(f"  Relocated: {relocated}")
    print(f"  Artifact docs updated: {artifacts_updated}")
    print(f"  Errors: {errors}")
    return 1 if errors > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
