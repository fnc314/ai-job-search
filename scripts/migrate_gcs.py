#!/usr/bin/env python3
"""ARCHIVED: One-time GCS migration, already completed. No Kotlin migration needed.

One-shot migration: upload local artifacts to GCS and backfill Firestore.

Scans job-applications/ for existing artifact files, uploads them to
GCS at applications/{company}/{role}/{YYYY-MM-DD}/{filename}, and backfills
the storage field on existing artifact documents in Firestore.

Usage:
    TRACKER_ALLOW_WRITES=1 python scripts/migrate_gcs.py          # Run migration
    python scripts/migrate_gcs.py --dry-run                       # Preview tree
    python scripts/migrate_gcs.py --company reddit --dry-run      # Preview one company
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
import artifacts as artifactsModule


def find_local_artifacts(documents_dir: pathlib.Path) -> list[dict]:
    """Scan job-applications/ for artifact files.

    Returns a list of dicts with keys: company, role, date, filename, path.
    """
    artifacts = []
    if not documents_dir.exists():
        return artifacts

    for company_dir in sorted(documents_dir.iterdir()):
        if not company_dir.is_dir() or company_dir.name.startswith("."):
            continue
        for role_dir in sorted(company_dir.iterdir()):
            if not role_dir.is_dir() or role_dir.name.startswith("."):
                continue
            for date_dir in sorted(role_dir.iterdir()):
                if not date_dir.is_dir() or date_dir.name.startswith("."):
                    continue
                for file_path in sorted(date_dir.iterdir()):
                    if file_path.is_file() and not file_path.name.startswith("."):
                        artifacts.append({
                            "company": company_dir.name,
                            "role": role_dir.name,
                            "date": date_dir.name,
                            "filename": file_path.name,
                            "path": file_path,
                        })
    return artifacts


def build_gcs_path(company: str, role: str, date_str: str, filename: str) -> str:
    """Build the GCS object path: applications/{company}/{role}/{date}/{filename}."""
    norm_company = contract.normalize_dir_name(company)
    norm_role = contract.normalize_dir_name(role)
    return f"applications/{norm_company}/{norm_role}/{date_str}/{filename}"


def build_firestore_index(db) -> dict[str, dict]:
    """Pre-build an index of all artifact documents for O(1) lookup.

    Returns a dict keyed by (company_lower, role_lower, filename_lower) -> artifact_doc.
    """
    index = {}
    for app_doc in db.collection(client.APPLICATIONS).stream():
        app_data = app_doc.to_dict()
        company = (app_data.get("company") or "").lower()
        role = (app_data.get("role") or "").lower()
        for art_doc in app_doc.reference.collection(client.ARTIFACTS).stream():
            art_data = art_doc.to_dict()
            filename = (art_data.get("filename") or "").lower()
            key = (company, role, filename)
            index[key] = {
                "doc": art_data,
                "ref": art_doc.reference,
                "app_id": app_doc.id,
            }
    return index


def find_artifact_in_index(index: dict, company: str, role: str, filename: str) -> dict | None:
    """Find a matching artifact document in the pre-built index."""
    key = (company.lower(), role.lower(), filename.lower())
    entry = index.get(key)
    if entry:
        return entry
    # Try with normalized names
    norm_company = contract.normalize_dir_name(company).lower()
    norm_role = contract.normalize_dir_name(role).lower()
    for (idx_company, idx_role, idx_filename), entry in index.items():
        if idx_company == norm_company and idx_role == norm_role and idx_filename == filename.lower():
            return entry
    return None


def _draw_tree(node: dict, prefix: str = "", is_last: bool = True) -> list[str]:
    """Draw a tree structure from a nested dict."""
    lines = []
    connector = "└── " if is_last else "├── "
    if "files" in node:
        for i, file_info in enumerate(node["files"]):
            f_last = i == len(node["files"]) - 1
            f_conn = "└── " if f_last else "├── "
            label = file_info["filename"]
            if file_info.get("has_storage"):
                label += " ✓"
            lines.append(f"{prefix}{f_conn}{label}")
        return lines

    for i, (name, child) in enumerate(node.items()):
        child_last = i == len(node) - 1
        child_conn = "└── " if child_last else "├── "
        extension = "    " if child_last else "│   "
        lines.append(f"{prefix}{child_conn}{name}/")
        lines.extend(_draw_tree(child, prefix + extension, child_last))
    return lines


def print_gcs_tree(artifacts: list[dict], firestore_index: dict | None) -> None:
    """Print a tree view of the GCS structure that will be created."""
    tree = {}
    for art in artifacts:
        norm_company = contract.normalize_dir_name(art["company"])
        norm_role = contract.normalize_dir_name(art["role"])

        company_node = tree.setdefault(norm_company, {})
        role_node = company_node.setdefault(norm_role, {})
        date_node = role_node.setdefault(art["date"], {})
        if "files" not in date_node:
            date_node["files"] = []

        has_storage = False
        if firestore_index:
            match = find_artifact_in_index(firestore_index, art["company"], art["role"], art["filename"])
            if match and match["doc"].get("storage"):
                has_storage = True

        date_node["files"].append({
            "filename": art["filename"],
            "has_storage": has_storage,
            "path": art["path"],
        })

    print(f"GCS Bucket: {client.ARTIFACT_BUCKET}")
    print(f"Prefix: applications/")
    print(f"Total files: {len(artifacts)}")
    print()
    print("applications/")
    for line in _draw_tree(tree, prefix="", is_last=True):
        print(f"  {line}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Migrate local artifacts to GCS")
    parser.add_argument("--dry-run", action="store_true", help="Show tree view without uploading")
    parser.add_argument("--company", help="Only process artifacts for this company (case-insensitive)")
    parser.add_argument("--documents", default=None, help="Path to job-applications/ directory")
    args = parser.parse_args()

    documents_dir = (
        pathlib.Path(args.documents).resolve()
        if args.documents
        else SCRIPT_DIR.parent / "documents" / "applications"
    )

    print(f"Scanning {documents_dir}...")
    artifacts = find_local_artifacts(documents_dir)

    if args.company:
        company_filter = args.company.lower()
        artifacts = [a for a in artifacts if contract.normalize_dir_name(a["company"]).lower() == company_filter]

    if not artifacts:
        print("No artifacts found.")
        return 0

    print(f"Found {len(artifacts)} artifacts")

    # Build Firestore index for matching
    db = client.getDb()
    firestore_index = build_firestore_index(db)
    print(f"Firestore index: {len(firestore_index)} artifact documents")

    if args.dry_run:
        print_gcs_tree(artifacts, firestore_index)
        return 0

    # Check write permission
    client.assertWritesAllowed("GCS migration")
    bucket = client.getBucket()

    uploaded = 0
    reused = 0
    backfilled = 0
    skipped = 0
    errors = 0

    for i, art in enumerate(artifacts, 1):
        gcs_path = build_gcs_path(art["company"], art["role"], art["date"], art["filename"])
        print(f"[{i}/{len(artifacts)}] {art['filename']} → {gcs_path}", flush=True)

        try:
            described = artifactsModule.describe(art["path"])
            # Upload directly to the GCS path, bypassing ensureUpload's blobPath
            blob = bucket.blob(gcs_path)
            if blob.exists():
                blob.reload()
                recorded = (blob.metadata or {}).get("sha256")
                if recorded and recorded != described["sha256"]:
                    print(f"  ✗ Hash mismatch, skipping", flush=True)
                    skipped += 1
                    continue
                if not recorded:
                    blob.metadata = {**(blob.metadata or {}), "sha256": described["sha256"], "size": str(described["size"])}
                    blob.patch()
                storage = {"bucket": bucket.name, "path": gcs_path}
                reused += 1
            else:
                blob.content_type = described["contentType"]
                blob.metadata = {"sha256": described["sha256"], "size": str(described["size"])}
                blob.upload_from_filename(str(art["path"]), if_generation_match=0)
                storage = {"bucket": bucket.name, "path": gcs_path}
                uploaded += 1

            # Backfill Firestore
            match = find_artifact_in_index(firestore_index, art["company"], art["role"], art["filename"])
            if match:
                match["ref"].set({"storage": storage}, merge=True)
                backfilled += 1
                print(f"  ✓ Backfilled storage on artifact doc", flush=True)
            else:
                print(f"  (No matching Firestore artifact found)", flush=True)

        except Exception as e:
            errors += 1
            print(f"  ✗ Error: {e}", flush=True)

        time.sleep(0.1)  # Rate limit

    print()
    print(f"Migration complete:")
    print(f"  Uploaded: {uploaded}")
    print(f"  Reused:   {reused}")
    print(f"  Backfilled: {backfilled}")
    print(f"  Errors:   {errors}")
    return 1 if errors > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
