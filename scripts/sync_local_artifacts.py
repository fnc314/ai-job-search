#!/usr/bin/env python3
"""Sync local job-applications files into Firestore artifact subcollections.

Scans job-applications/ for PDF, MD, and TXT files, matches them to Firestore
application documents, and writes/updates the applications/{id}/artifacts
subcollection with proper metadata (sha256, size, contentType, kind).

Usage:
    mise run tracker:artifacts:sync [--dry-run] [--company <company>]

Environment:
    TRACKER_ALLOW_WRITES=1  Required for live writes (otherwise dry-run only).
"""

from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import os
import sys
from pathlib import Path

# ── Repo root resolution ────────────────────────────────────────────────

def find_repo_root() -> Path:
    root = Path(__file__).resolve().parent.parent
    while root != root.parent and not (root / "mise.toml").exists():
        root = root.parent
    return root


# ── Firestore client (lazy) ─────────────────────────────────────────────

def get_firestore():
    """Return a Firestore client, importing firebase-admin only when needed."""
    import firebase_admin
    from firebase_admin import credentials, firestore

    if not firebase_admin._apps:
        cred_path = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
        if cred_path:
            cred = credentials.Certificate(cred_path)
        else:
            cred = credentials.ApplicationDefault()
        firebase_admin.initialize_app(cred)
    return firestore.client()


# ── Helpers ──────────────────────────────────────────────────────────────

def file_sha256(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def classify_kind(filename: str) -> str:
    """Classify artifact kind from filename."""
    name = filename.lower()
    if name.startswith("main_") or "cv" in name or "resume" in name:
        return "CV"
    if name.startswith("cover_") or "cover_letter" in name:
        return "COVER_LETTER"
    if name == "job_posting.md":
        return "POSTING"
    if name == "outcome.md":
        return "OUTCOME"
    if name.endswith(".md") or name.endswith(".txt"):
        return "NOTE"
    return "OTHER"


def content_type(path: Path) -> str:
    guess, _ = mimetypes.guess_type(str(path))
    return guess or "application/octet-stream"


def slugify(text: str) -> str:
    """Normalize company/role name to Firestore ID style."""
    import re
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    text = re.sub(r"[\s_]+", "-", text)
    return text.strip("-")


# ── Core logic ───────────────────────────────────────────────────────────

def find_applications(db) -> dict[str, dict]:
    """Load all applications from Firestore, keyed by ID."""
    docs = db.collection("applications").stream()
    result = {}
    for doc in docs:
        data = doc.to_dict()
        if data:
            result[doc.id] = data
    return result


def match_application(company: str, role: str, applications: dict[str, dict]) -> str | None:
    """Try to match a local company/role path to a Firestore application ID."""
    company_slug = slugify(company)

    # Direct match on company field
    for app_id, app_data in applications.items():
        app_company = (app_data.get("company") or "").lower()
        app_role = (app_data.get("role") or "").lower()
        if slugify(app_company) == company_slug:
            # If role also matches, prefer it
            if not role or slugify(app_role) == slugify(role):
                return app_id

    # Fallback: first match on company alone
    for app_id, app_data in applications.items():
        app_company = (app_data.get("company") or "").lower()
        if slugify(app_company) == company_slug:
            return app_id

    return None


def get_existing_artifacts(db, app_id: str) -> dict[str, dict]:
    """Load existing artifacts for an application, keyed by filename."""
    artifacts = {}
    try:
        docs = db.collection(f"applications/{app_id}/artifacts").stream()
        for doc in docs:
            data = doc.to_dict()
            if data and data.get("filename"):
                artifacts[data["filename"]] = data
    except Exception:
        pass
    return artifacts


def sync_file(
    file_path: Path,
    app_id: str,
    existing: dict[str, dict],
    db,
    dry_run: bool,
    repo_root: Path,
) -> dict:
    """Sync a single file to Firestore. Returns change summary."""
    filename = file_path.name
    rel_path = str(file_path.relative_to(repo_root))
    file_size = file_path.stat().st_size
    sha = file_sha256(file_path)
    ct = content_type(file_path)
    kind = classify_kind(filename)

    existing_artifact = existing.get(filename)
    changed = False
    reason = ""

    if existing_artifact:
        # Check if anything changed
        if (existing_artifact.get("sha256") != sha or
            existing_artifact.get("size") != file_size or
            existing_artifact.get("localPath") != rel_path or
            existing_artifact.get("kind") != kind):
            changed = True
            reason = "updated"
    else:
        changed = True
        reason = "added"

    if not changed:
        return {"status": "unchanged", "file": str(file_path)}

    artifact_doc = {
        "artifactId": existing_artifact.get("artifactId"),
        "kind": kind,
        "filename": filename,
        "contentType": ct,
        "size": file_size,
        "sha256": sha,
        "localPath": rel_path,
        "createdAt": existing_artifact.get("createdAt"),
    }

    if dry_run:
        return {
            "status": reason,
            "file": str(file_path),
            "app_id": app_id,
            "kind": kind,
            "size": file_size,
            "sha256": sha,
        }

    # Write to Firestore
    collection = db.collection(f"applications/{app_id}/artifacts")
    if existing_artifact.get("artifactId"):
        # Update existing document
        doc_id = None
        for doc in collection.stream():
            doc_data = doc.to_dict()
            if doc_data and doc_data.get("filename") == filename:
                doc_id = doc.id
                break
        if doc_id:
            collection.document(doc_id).set(artifact_doc, merge=True)
        else:
            # Generate new ID
            import uuid
            new_id = str(uuid.uuid4())
            artifact_doc["artifactId"] = new_id
            collection.document(new_id).set(artifact_doc)
    else:
        import uuid
        new_id = str(uuid.uuid4())
        artifact_doc["artifactId"] = new_id
        collection.document(new_id).set(artifact_doc)

    return {
        "status": reason,
        "file": str(file_path),
        "app_id": app_id,
        "kind": kind,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync local job-applications files to Firestore")
    parser.add_argument("--dry-run", action="store_true", help="Preview without writing")
    parser.add_argument("--company", help="Limit to a specific company directory")
    args = parser.parse_args()

    repo_root = find_repo_root()
    job_apps_dir = repo_root / "job-applications"

    if not job_apps_dir.exists():
        print(f"Error: {job_apps_dir} does not exist")
        return 1

    # Check write permission
    allow_writes = os.environ.get("TRACKER_ALLOW_WRITES") == "1"
    dry_run = args.dry_run or not allow_writes

    if dry_run and not args.dry_run:
        print("Dry-run mode (set TRACKER_ALLOW_WRITES=1 to enable writes)")

    # Load Firestore applications
    print("Loading applications from Firestore...")
    db = get_firestore()
    applications = find_applications(db)
    print(f"Found {len(applications)} applications in Firestore")

    # Scan local files
    extensions = {".pdf", ".md", ".txt"}
    files_to_sync = []

    for company_dir in sorted(job_apps_dir.iterdir()):
        if not company_dir.is_dir():
            continue
        if args.company and slugify(company_dir.name) != slugify(args.company):
            continue

        for role_dir in sorted(company_dir.iterdir()):
            if not role_dir.is_dir():
                continue

            for date_dir in sorted(role_dir.iterdir()):
                if not date_dir.is_dir():
                    continue

                for file_path in sorted(date_dir.iterdir()):
                    if file_path.suffix.lower() in extensions:
                        files_to_sync.append((company_dir.name, role_dir.name, file_path))

    print(f"Found {len(files_to_sync)} local files to sync")

    if not files_to_sync:
        print("Nothing to do.")
        return 0

    # Sync files
    results = {"added": 0, "updated": 0, "unchanged": 0, "skipped": 0}

    for company, role, file_path in files_to_sync:
        app_id = match_application(company, role, applications)
        if not app_id:
            print(f"  SKIP {file_path.relative_to(repo_root)} (no matching application)")
            results["skipped"] += 1
            continue

        existing = get_existing_artifacts(db, app_id)
        result = sync_file(file_path, app_id, existing, db, dry_run, repo_root)
        status = result["status"]
        results[status] = results.get(status, 0) + 1

        rel = file_path.relative_to(repo_root)
        if status != "unchanged":
            print(f"  {status.upper():7s} {rel} -> {app_id} ({result.get('kind', '?')})")

    # Summary
    print(f"\nSync complete:")
    print(f"  Added:     {results.get('added', 0)}")
    print(f"  Updated:   {results.get('updated', 0)}")
    print(f"  Unchanged: {results.get('unchanged', 0)}")
    print(f"  Skipped:   {results.get('skipped', 0)}")

    if dry_run:
        print("\n(Run with TRACKER_ALLOW_WRITES=1 to apply changes)")

    return 0


if __name__ == "__main__":
    sys.exit(main())
