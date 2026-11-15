#!/usr/bin/env python3
"""DEPRECATED: Firestore restore — retained as Python, simple utility.

Full offline restore utility for Firestore job-tracker database.
Restores all collections and subcollections from a backup JSON file,
reverting database state exactly.
"""

import argparse
import datetime
import json
import os
import sys
from typing import Any

import client
from google.cloud.firestore_v1._helpers import GeoPoint


def deserialize_val(val: Any) -> Any:
    if isinstance(val, dict):
        if val.get("__type__") == "timestamp" and "value" in val:
            return datetime.datetime.fromisoformat(val["value"])
        elif val.get("__type__") == "geopoint":
            return GeoPoint(val["latitude"], val["longitude"])
        return {k: deserialize_val(v) for k, v in val.items()}
    elif isinstance(val, list):
        return [deserialize_val(x) for x in val]
    return val


def restore(backup_path: str, dry_run: bool = True) -> None:
    if not os.path.exists(backup_path):
        raise FileNotFoundError(f"Backup file not found: {backup_path}")

    print(f"Reading backup from: {backup_path}")
    with open(backup_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    meta = data.get("metadata", {})
    counts = meta.get("counts", {})
    print(f"Backup Metadata: Timestamp={meta.get('timestamp')}, Version={meta.get('version')}")
    print(f"Document counts in backup: {counts}")

    if dry_run:
        print("[DRY-RUN] Restore simulation finished successfully. No writes were executed.")
        return

    client.assertWritesAllowed()
    db = client.getDb()
    batch = db.batch()
    batch_count = 0
    total_committed = 0

    def commit_batch_if_needed():
        nonlocal batch, batch_count, total_committed
        if batch_count >= 400:
            batch.commit()
            total_committed += batch_count
            print(f"Committed batch of {batch_count} writes (Total: {total_committed})")
            batch = db.batch()
            batch_count = 0

    # 1. Restore applications
    print("Restoring applications...")
    for app_id, app_data in data.get("applications", {}).items():
        doc_ref = db.collection(client.APPLICATIONS).document(app_id)
        batch.set(doc_ref, deserialize_val(app_data), merge=False)
        batch_count += 1
        commit_batch_if_needed()

    # 2. Restore events
    print("Restoring events...")
    for key, ev_data in data.get("events", {}).items():
        app_id, ev_id = key.split("/", 1)
        doc_ref = db.collection(client.APPLICATIONS).document(app_id).collection(client.EVENTS).document(ev_id)
        batch.set(doc_ref, deserialize_val(ev_data), merge=False)
        batch_count += 1
        commit_batch_if_needed()

    # 3. Restore artifacts
    print("Restoring artifacts...")
    for key, art_data in data.get("artifacts", {}).items():
        app_id, art_id = key.split("/", 1)
        doc_ref = db.collection(client.APPLICATIONS).document(app_id).collection(client.ARTIFACTS).document(art_id)
        batch.set(doc_ref, deserialize_val(art_data), merge=False)
        batch_count += 1
        commit_batch_if_needed()

    # 4. Restore pending proposals
    print("Restoring pending proposals...")
    for prop_id, prop_data in data.get("pending_proposals", {}).items():
        doc_ref = db.collection(client.PENDING_PROPOSALS).document(prop_id)
        batch.set(doc_ref, deserialize_val(prop_data), merge=False)
        batch_count += 1
        commit_batch_if_needed()

    # 5. Restore sync requests
    print("Restoring sync requests...")
    for sr_id, sr_data in data.get("sync_requests", {}).items():
        doc_ref = db.collection(client.SYNC_REQUESTS).document(sr_id)
        batch.set(doc_ref, deserialize_val(sr_data), merge=False)
        batch_count += 1
        commit_batch_if_needed()

    # 6. Restore companies
    print("Restoring companies...")
    for comp_id, comp_data in data.get("companies", {}).items():
        doc_ref = db.collection(client.COMPANIES).document(comp_id)
        batch.set(doc_ref, deserialize_val(comp_data), merge=False)
        batch_count += 1
        commit_batch_if_needed()

    # 7. Restore contacts
    print("Restoring contacts...")
    for cont_id, cont_data in data.get("contacts", {}).items():
        doc_ref = db.collection(client.CONTACTS).document(cont_id)
        batch.set(doc_ref, deserialize_val(cont_data), merge=False)
        batch_count += 1
        commit_batch_if_needed()

    # Final commit
    if batch_count > 0:
        batch.commit()
        total_committed += batch_count
        print(f"Final batch of {batch_count} writes committed.")

    print(f"SUCCESS: Total {total_committed} documents restored from backup!")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Restore Firestore database from backup JSON")
    parser.add_argument("--backup", required=True, help="Path to backup JSON file")
    parser.add_argument("--apply", action="store_true", help="Execute writes to Firestore (default is dry-run)")
    args = parser.parse_args()

    restore(args.backup, dry_run=not args.apply)
