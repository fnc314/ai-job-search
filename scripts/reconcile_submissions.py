#!/usr/bin/env python3
"""Database reconciliation and cryptographic hash-chain backfill.

1. Backfills sequenceNumber, prevEventHash, and eventHash across all historical events in Firestore.
2. Synchronizes on-disk applications from job-applications/ that were completed but missing from Firestore:
   - aarp--mobile-tech-lead (2026-11-04)
   - marriott--principal-engineer-android-platform (2026-11-03)
   - patientpoint--senior-staff-software-engineer-android-iot (2026-11-05)
   - patientpoint--sr-manager-software-engineering-android-device-platforms (2026-11-09)
   - capital-one--senior-lead-software-engineer-android (2026-10-26)
   - tekgence--android-mobile-developer (2026-10-23)
3. Resolves Capital One status:
   - capital-one--android-mobile-engineer-4 (R1001499) -> REJECTED
   - capital-one--senior-lead-software-engineer-android (R248778) -> REJECTED
   - capital-one--mobile-engineer-4-android (R1001320) -> INTERVIEW (Recruiter-led, Daniel Poland)
4. Projects submittedAt, submissionConfirmedAt, and submissionConfirmation for all applications.
"""

from __future__ import annotations

import argparse
import datetime as dt
import os
import sys
from pathlib import Path

import client
import contract
import events as eventsModule
import render


def reconcile(dry_run: bool = False):
    db = client.getDb()
    print(f"Connecting to Firestore (dry_run={dry_run})...")

    # Step 1: Create missing applications if they do not exist
    missing_definitions = [
        {
            "id": "aarp--mobile-tech-lead",
            "company": "AARP",
            "role": "Mobile Tech Lead",
            "location": "Washington, DC (Hybrid)",
            "date": "2026-11-04T11:10:00Z",
            "status": "APPLIED",
            "channel": "OUTREACH_LINKEDIN",
            "jobUrl": "https://apply.aarp.org/careers/job/1443153978791?domain=aarp.org&src=LinkedIn",
            "fitRating": 78,
            "events": [
                {
                    "type": "APPLY",
                    "date": "2026-11-04T11:10:00Z",
                    "actor": "USER",
                    "note": "Applied via AARP career portal",
                },
                {
                    "type": "ACKNOWLEDGE",
                    "date": "2026-11-04T11:13:39Z",
                    "actor": "EMAIL_SYNC",
                    "note": "Franco, we have received your application",
                },
            ],
        },
        {
            "id": "marriott--principal-engineer-android-platform",
            "company": "Marriott",
            "role": "Principal Engineer, Android Platform",
            "location": "Bethesda, MD (Hybrid)",
            "date": "2026-11-03T12:00:00Z",
            "status": "APPLIED",
            "channel": "COMPANY_WEBSITE",
            "jobUrl": "https://careers.marriott.com/principal-engineer-android-platform/job/P1-7062993-0",
            "fitRating": 95,
            "events": [
                {
                    "type": "APPLY",
                    "date": "2026-11-03T12:00:00Z",
                    "actor": "USER",
                    "note": "Applied via Marriott career portal",
                },
                {
                    "type": "ACKNOWLEDGE",
                    "date": "2026-11-03T12:05:00Z",
                    "actor": "EMAIL_SYNC",
                    "note": "Application confirmation received",
                },
            ],
        },
        {
            "id": "patientpoint--senior-staff-software-engineer-android-iot",
            "company": "PatientPoint",
            "role": "Senior Staff Software Engineer, Android & IoT",
            "location": "Remote (US)",
            "date": "2026-11-05T14:00:00Z",
            "status": "APPLIED",
            "channel": "COMPANY_WEBSITE",
            "jobId": "4751430005",
            "fitRating": 72,
            "events": [
                {
                    "type": "APPLY",
                    "date": "2026-11-05T14:00:00Z",
                    "actor": "USER",
                    "note": "Applied via PatientPoint Careers",
                },
            ],
        },
        {
            "id": "patientpoint--sr-manager-software-engineering-android-device-platforms",
            "company": "PatientPoint",
            "role": "Sr. Manager, Software Engineering - Android & Device Platforms",
            "location": "Remote (US)",
            "date": "2026-11-09T15:00:00Z",
            "status": "APPLIED",
            "channel": "COMPANY_WEBSITE",
            "fitRating": 94,
            "events": [
                {
                    "type": "APPLY",
                    "date": "2026-11-09T15:00:00Z",
                    "actor": "USER",
                    "note": "Applied via PatientPoint Careers",
                },
            ],
        },
        {
            "id": "capital-one--senior-lead-software-engineer-android",
            "company": "Capital One",
            "role": "Senior Lead Software Engineer, Android (R248778)",
            "location": "McLean, VA / Remote",
            "date": "2026-10-26T00:00:00Z",
            "status": "REJECTED",
            "channel": "COMPANY_WEBSITE",
            "jobId": "R248778",
            "fitRating": 90,
            "events": [
                {
                    "type": "APPLY",
                    "date": "2026-10-26T00:00:00Z",
                    "actor": "USER",
                    "note": "Applied for Req R248778",
                },
                {
                    "type": "REJECT",
                    "date": "2026-10-29T19:22:41Z",
                    "actor": "EMAIL_SYNC",
                    "statusTo": "REJECTED",
                    "note": "We've closed your application—here's what to know",
                },
            ],
        },
        {
            "id": "tekgence--android-mobile-developer",
            "company": "Tekgence",
            "role": "Android Mobile Developer",
            "location": "Pittsburgh, PA (Onsite)",
            "date": "2026-10-23T00:00:00Z",
            "status": "DRAFTING",
            "channel": "RECRUITER",
            "fitRating": 70,
            "events": [
                {
                    "type": "ADD_NOTE",
                    "date": "2026-10-23T00:00:00Z",
                    "actor": "USER",
                    "note": "Recruiter outreach from Saurabh Maurya for onsite role",
                },
            ],
        },
    ]

    for m in missing_definitions:
        doc_ref = db.collection("applications").document(m["id"])
        snap = doc_ref.get()
        if not snap.exists:
            print(f"Creating missing application: {m['id']}")
            status_fields = contract.deriveStatusFields(m["status"])
            app_data = {
                "id": m["id"],
                "company": m["company"],
                "role": m["role"],
                "status": status_fields["status"],
                "statusRank": status_fields["statusRank"],
                "isOpen": status_fields["isOpen"],
                "date": m["date"],
                "channel": m["channel"],
                "fitRating": m.get("fitRating"),
                "location": {"city": m["location"]},
                "tags": [],
                "contactIds": [],
                "emailMessageIds": [],
                "schemaVersion": 10,
                "createdAt": m["date"],
                "updatedAt": render.nowIso() if hasattr(render, "nowIso") else m["date"],
            }
            if m.get("jobId"):
                app_data["jobId"] = m["jobId"]
            if m.get("jobUrl"):
                app_data["jobUrl"] = m["jobUrl"]

            if not dry_run:
                doc_ref.set(contract.toFirestore(app_data))
                for ev in m.get("events", []):
                    built = eventsModule.buildEvent(
                        ev["type"],
                        date=ev["date"],
                        note=ev.get("note"),
                        actor=ev.get("actor", "USER"),
                        statusTo=ev.get("statusTo"),
                    )
                    ev_ref = doc_ref.collection("events").document(built["eventId"])
                    ev_ref.set(contract.toFirestore(built))
            print(f"  -> Created {m['id']} with {len(m.get('events', []))} events")

    # Step 2: Handle Capital One statuses
    c1_4 = db.collection("applications").document("capital-one--android-mobile-engineer-4").get()
    if c1_4.exists:
        data = c1_4.to_dict()
        if data.get("status") != "REJECTED":
            print("Updating capital-one--android-mobile-engineer-4 to REJECTED")
            if not dry_run:
                rej_fields = contract.deriveStatusFields("REJECTED")
                db.collection("applications").document("capital-one--android-mobile-engineer-4").update({
                    "status": rej_fields["status"],
                    "statusRank": rej_fields["statusRank"],
                    "isOpen": rej_fields["isOpen"],
                })
                # Add REJECT event if not present
                ev_col = db.collection("applications").document("capital-one--android-mobile-engineer-4").collection("events")
                ev_types = [e.to_dict().get("type") for e in ev_col.stream()]
                if "REJECT" not in ev_types and "event:rejection" not in ev_types:
                    built = eventsModule.buildEvent(
                        "REJECT",
                        date="2026-10-27T00:00:00Z",
                        actor="EMAIL_SYNC",
                        statusTo="REJECTED",
                        note="Rejection based on assessment results",
                    )
                    ev_col.document(built["eventId"]).set(contract.toFirestore(built))

    c1_nov = db.collection("applications").document("capital-one--mobile-engineer-4-android").get()
    if c1_nov.exists:
        data = c1_nov.to_dict()
        if data.get("status") != "INTERVIEW":
            print("Updating capital-one--mobile-engineer-4-android to INTERVIEW")
            if not dry_run:
                int_fields = contract.deriveStatusFields("INTERVIEW")
                db.collection("applications").document("capital-one--mobile-engineer-4-android").update({
                    "status": int_fields["status"],
                    "statusRank": int_fields["statusRank"],
                    "isOpen": int_fields["isOpen"],
                })

    # Step 3: Backfill and verify event hash chains for ALL applications
    print("\nBackfilling event hash chains across all applications...")
    all_apps = list(db.collection("applications").stream())
    events_rehashed = 0
    apps_reprojected = 0

    for doc in all_apps:
        app_id = doc.id
        app_data = doc.to_dict()
        ev_col = db.collection("applications").document(app_id).collection("events")
        raw_events = [e.to_dict() for e in ev_col.stream()]
        if not raw_events:
            continue

        # Chain events
        chained = eventsModule.chainEvents(raw_events, app_id)
        events_rehashed += len(chained)

        # Write chained events back to Firestore
        if not dry_run:
            batch = db.batch()
            for ev in chained:
                ev_ref = ev_col.document(ev["eventId"])
                batch.set(ev_ref, contract.toFirestore(ev))
            batch.commit()

        # Check chain validity
        if not eventsModule.verifyEventChain(chained, app_id):
            print(f"ERROR: Hash chain verification failed for {app_id}!")
        else:
            # Recompute projected fields
            proj = render.project(app_data, chained)
            proj["schemaVersion"] = 10
            
            # Special case for recruiter-led Capital One Nov role
            if app_id == "capital-one--mobile-engineer-4-android":
                proj["submissionConfirmation"] = {
                    "isConfirmed": True,
                    "confidence": "RECRUITER_LED",
                    "confirmedAt": "2026-11-04T03:31:47+00:00",
                    "confirmationEmailMessageId": "1a1a49df3ce93612",
                    "sourceSubject": "daniel.poland@capitalone.com",
                    "notes": "Recruiter-led candidate interaction and portal submission",
                }
                proj["timeline"]["submittedAt"] = "2026-11-04T03:31:47+00:00"
                proj["timeline"]["submissionConfirmedAt"] = "2026-11-04T03:31:47+00:00"

            if not dry_run:
                db.collection("applications").document(app_id).update(contract.toFirestore(proj))
            apps_reprojected += 1

    print(f"\nReconciliation finished!")
    print(f"Total events cryptographically chained: {events_rehashed}")
    print(f"Total applications reprojected to schema v10: {apps_reprojected}")


def main():
    parser = argparse.ArgumentParser(description="Reconcile official submissions and event hash chains")
    parser.add_argument("--dry-run", action="store_true", help="Preview changes without writing to Firestore")
    args = parser.parse_args()
    reconcile(dry_run=args.dry_run)


if __name__ == "__main__":
    main()
