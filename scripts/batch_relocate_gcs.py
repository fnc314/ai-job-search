#!/usr/bin/env python3
"""Batch relocate all GCS blobs from Title_Case to kebab-case slugs.

This script migrates all GCS blobs from the legacy Title_Case naming scheme
(e.g., `applications/Butterfly_Network/...`) to the canonical kebab-case
scheme (e.g., `applications/butterfly-network/...`) that matches Firestore
document IDs.

Usage:
    python scripts/batch_relocate_gcs.py --dry-run
    TRACKER_ALLOW_WRITES=1 python scripts/batch_relocate_gcs.py
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

# Canonical mapping from GCS Title_Case slugs to Firestore kebab-case slugs
SLUG_MAPPING = {
    "Anthropic": "anthropic",
    "Bankjoy": "bankjoy",
    "Bigcommerce": "bigcommerce",
    "Bitwarden": "bitwarden",
    "Block": "block",
    "Butterfly_Network": "butterfly-network",
    "Canary_Technologies": "canary-technologies",
    "Capital_One": "capital-one",
    "Carnegie_Mellon_University": "carnegie-mellon-university",
    "Centene": "centene",
    "Classpass": "classpass",
    "Coinbase": "coinbase",
    "Crestron_Electronics": "crestron-electronics",
    "Deloitte": "deloitte",
    "Disney": "disney",
    "Doordash": "doordash",
    "Dropbox": "dropbox",
    "Duolingo": "duolingo",
    "Edward_Jones": "edward-jones",
    "Eight_Sleep": "eight-sleep",
    "Fanatics": "fanatics",
    "Fetch": "fetch",
    "Ford_Motor_Company": "ford",
    "Future": "future",
    "General_Motors": "general-motors",
    "Github": "github",
    "Google": "google",
    "Grove_Collaborative": "grove-collaborative",
    "Grow_Therapy": "grow-therapy",
    "Hims_Hers": "hims-hers",
    "Honor": "honor",
    "Huntington": "huntington",
    "Iherb": "iherb",
    "Instacart": "instacart",
    "Jpmorgan_Chase": "jpmorgan-chase",
    "Karoo_Health": "karoo-health",
    "Lime": "lime",
    "Major_League_Baseball": "major-league-baseball",
    "Medium": "medium",
    "Meta": "meta",
    "Microsoft": "microsoft",
    "Nextdoor": "nextdoor",
    "Npr": "npr",
    "Onx": "onx",
    "Openai": "openai",
    "Osmind": "osmind",
    "Paramount": "paramount",
    "Playlist": "playlist",
    "Pnc": "pnc",
    "Radar": "radar",
    "Ramp": "ramp",
    "Reddit": "reddit",
    "Revenuecat": "revenuecat",
    "Robinhood": "robinhood",
    "Roku": "roku",
    "Salesforce": "salesforce",
    "Scout_Motors": "scout-motors",
    "Seatgeek": "seatgeek",
    "Sentilink": "sentilink",
    "Sharkninja": "sharkninja",
    "Slickdeals": "slickdeals",
    "Snap_Inc": "snap",
    "Speechify": "speechify",
    "Spoton": "spoton",
    "Teamsnap": "teamsnap",
    "Tekgence": "tekgence",
    "Textnow": "textnow",
    "The_Trade_Desk": "trade-desk",
    "Thomson_Reuters": "thomson-reuters",
    "Toast": "toast",
    "Touchlab": "touchlab",
    "Ulta_Beauty": "ulta-beauty",
    "Unit21": "unit21",
    "Waymo": "waymo",
    "Yinzcam": "yinzcam",
    "Zillow": "zillow",
    "Ziprecruiter": "ziprecruiter",
}


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


def collect_all_artifact_paths(db) -> dict[str, str]:
    """Collect all artifact GCS paths and return {old_path: artifact_doc_path}."""
    paths = {}
    for app_doc in db.collection(client.APPLICATIONS).stream():
        for art_doc in app_doc.reference.collection(client.ARTIFACTS).stream():
            art_data = art_doc.to_dict()
            if not art_data:
                continue
            storage = art_data.get("storage") or {}
            if isinstance(storage, dict):
                stored_path = storage.get("path", "")
                if stored_path:
                    paths[stored_path] = art_doc.reference.path
    return paths


def batch_update_artifacts(db, path_mapping: dict[str, str]) -> int:
    """Batch update all artifact documents whose paths changed."""
    updated = 0
    for old_path, new_path in path_mapping.items():
        try:
            art_ref = db.document(old_path)
            art_data = art_ref.get().to_dict()
            if art_data:
                storage = art_data.get("storage") or {}
                if isinstance(storage, dict) and storage.get("path") == old_path:
                    storage["path"] = new_path
                    art_ref.set({"storage": storage}, merge=True)
                    updated += 1
        except Exception:
            pass
    return updated


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Batch relocate GCS blobs from Title_Case to kebab-case slugs"
    )
    parser.add_argument(
        "--dry-run", action="store_true", help="Preview relocations without moving blobs"
    )
    args = parser.parse_args()

    db = client.getDb()
    bucket = client.getBucket()

    # Collect all blobs to relocate
    relocations = []
    for old_slug, new_slug in SLUG_MAPPING.items():
        old_prefix = f"applications/{old_slug}/"
        new_prefix = f"applications/{new_slug}/"
        blobs = list(bucket.list_blobs(prefix=old_prefix))
        for blob in blobs:
            new_path = blob.name.replace(old_prefix, new_prefix, 1)
            relocations.append((blob.name, new_path, old_slug, new_slug))

    if not relocations:
        print("No blobs to relocate.")
        return 0

    if args.dry_run:
        # Group by company for summary
        by_company = defaultdict(list)
        for old_path, new_path, old_slug, new_slug in relocations:
            by_company[f"{old_slug} → {new_slug}"].append((old_path, new_path))

        for company_pair, paths in sorted(by_company.items()):
            print(f"\n{company_pair}:")
            for old_path, new_path in paths[:3]:
                print(f"  {old_path}")
                print(f"    → {new_path}")
            if len(paths) > 3:
                print(f"  ... and {len(paths) - 3} more")

        print(f"\nTotal blobs to relocate: {len(relocations)}")
        return 0

    # Check write permission
    client.assertWritesAllowed("GCS blob batch relocation")

    # Collect all artifact paths upfront (single pass)
    print("Collecting artifact references...")
    artifact_paths = collect_all_artifact_paths(db)
    print(f"  Found {len(artifact_paths)} artifact references")

    relocated = 0
    errors = 0

    print(f"\nRelocating {len(relocations)} blobs...")
    print()

    # Build path mapping for batch artifact update
    path_mapping: dict[str, str] = {}

    for i, (old_path, new_path, old_slug, new_slug) in enumerate(relocations, 1):
        print(
            f"  [{i}/{len(relocations)}] {old_slug} → {new_slug}: {pathlib.Path(old_path).name}",
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
        print(f"\nUpdating {len(path_mapping)} artifact references...")
        artifacts_updated = batch_update_artifacts(db, path_mapping)
        print(f"  Updated {artifacts_updated} artifact documents")

    print(f"\nRelocation complete:")
    print(f"  Relocated: {relocated}")
    print(f"  Artifact docs updated: {artifacts_updated}")
    print(f"  Errors: {errors}")
    return 1 if errors > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
