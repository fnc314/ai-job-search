#!/usr/bin/env python3
"""
Full offline backup utility for Firestore job-tracker database.
Dumps all collections and subcollections to a structured JSON file
with document counts and verification checksums.
"""

import argparse
import datetime
import hashlib
import json
import os
import sys
from typing import Any

import client
from google.cloud.firestore_v1._helpers import GeoPoint
from google.api_core.datetime_helpers import DatetimeWithNanoseconds


def serialize_val(val: Any) -> Any:
    if isinstance(val, (datetime.datetime, DatetimeWithNanoseconds)):
        return {"__type__": "timestamp", "value": val.isoformat()}
    elif isinstance(val, GeoPoint):
        return {"__type__": "geopoint", "latitude": val.latitude, "longitude": val.longitude}
    elif isinstance(val, dict):
        return {k: serialize_val(v) for k, v in val.items()}
    elif isinstance(val, list):
        return [serialize_val(x) for x in val]
    return val


def backup() -> str:
    db = client.getDb()
    timestamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y%m%d_%H%M%S")
    out_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "generated")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, f"backup_v7_{timestamp}.json")

    print(f"Starting Firestore backup to: {out_path}")
    data = {
        "metadata": {
            "timestamp": timestamp,
            "project": db.project,
            "version": 7,
        },
        "applications": {},
        "events": {},  # keyed by f"{app_id}/{event_id}"
        "artifacts": {},  # keyed by f"{app_id}/{artifact_id}"
        "pending_proposals": {},
        "sync_requests": {},
        "companies": {},
        "contacts": {},
    }

    # 1. Applications and Subcollections
    print("Exporting applications and nested events/artifacts...")
    apps = list(db.collection(client.APPLICATIONS).stream())
    for app in apps:
        app_id = app.id
        app_dict = serialize_val(app.to_dict() or {})
        data["applications"][app_id] = app_dict

        # Events
        for ev in app.reference.collection(client.EVENTS).stream():
            key = f"{app_id}/{ev.id}"
            data["events"][key] = serialize_val(ev.to_dict() or {})

        # Artifacts
        for art in app.reference.collection(client.ARTIFACTS).stream():
            key = f"{app_id}/{art.id}"
            data["artifacts"][key] = serialize_val(art.to_dict() or {})

    # 2. Pending Proposals
    print("Exporting pending proposals...")
    for prop in db.collection(client.PENDING_PROPOSALS).stream():
        data["pending_proposals"][prop.id] = serialize_val(prop.to_dict() or {})

    # 3. Sync Requests
    print("Exporting sync requests...")
    for sr in db.collection(client.SYNC_REQUESTS).stream():
        data["sync_requests"][sr.id] = serialize_val(sr.to_dict() or {})

    # 4. Companies
    print("Exporting companies...")
    for comp in db.collection(client.COMPANIES).stream():
        data["companies"][comp.id] = serialize_val(comp.to_dict() or {})

    # 5. Contacts
    print("Exporting contacts...")
    for cont in db.collection(client.CONTACTS).stream():
        data["contacts"][cont.id] = serialize_val(cont.to_dict() or {})

    counts = {
        "applications": len(data["applications"]),
        "events": len(data["events"]),
        "artifacts": len(data["artifacts"]),
        "pending_proposals": len(data["pending_proposals"]),
        "sync_requests": len(data["sync_requests"]),
        "companies": len(data["companies"]),
        "contacts": len(data["contacts"]),
    }
    data["metadata"]["counts"] = counts

    raw_json = json.dumps(data, indent=2, sort_keys=True)
    sha256 = hashlib.sha256(raw_json.encode("utf-8")).hexdigest()
    data["metadata"]["sha256"] = sha256

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(json.dumps(data, indent=2, sort_keys=True))

    print(f"Backup complete!")
    print(f"Exported counts: {counts}")
    print(f"SHA-256: {sha256}")
    print(f"Saved to: {out_path}")
    return out_path


if __name__ == "__main__":
    backup()
