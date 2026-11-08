#!/usr/bin/env python3
"""
scripts/reconcile_withdrawn_postings.py

Reconciles the 10 erroneous WITHDRAWN application records created on 2026-11-05:
1. Takes an automated snapshot backup of Firestore before mutating.
2. Updates `ebay--staff-android-engineer-mts-2`:
   - Transitions status to DRAFTING (statusRank: 1, isOpen: True).
   - Registers local artifacts from `job-applications/ebay/staff-android-engineer-mts-2/2026-11-06/`.
   - Records an event documenting the transition to DRAFTING.
3. Deletes the 9 non-application scraped records from `applications`:
   - `whoop--staff-android-engineer-connectivity`
   - `echostar--staff-android-software-engineer`
   - `echostar--staff-android-tv-ui-engineer`
   - `qualcomm--staff-software-engineer-mobile-android`
   - `rakuten-rewards--principal-android-engineer`
   - `real--senior-principal-mobile-software-architect`
   - `andpad-vietnam--hn-hcm-hybrid-principal-android-engineer`
   - `movilges--senior-mobile-architect`
   - `termius--mobile-tech-lead-architect-ios-android`
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO_ROOT / "scripts"))

import backup_firestore
import client
import contract
import events as events_module
import render

EBAY_APP_ID = "ebay--staff-android-engineer-mts-2"
EBAY_ARTIFACTS_DIR = REPO_ROOT / "job-applications" / "ebay" / "staff-android-engineer-mts-2" / "2026-11-06"

DELETE_APP_IDS = [
    "whoop--staff-android-engineer-connectivity",
    "echostar--staff-android-software-engineer",
    "echostar--staff-android-tv-ui-engineer",
    "qualcomm--staff-software-engineer-mobile-android",
    "rakuten-rewards--principal-android-engineer",
    "real--senior-principal-mobile-software-architect",
    "andpad-vietnam--hn-hcm-hybrid-principal-android-engineer",
    "movilges--senior-mobile-architect",
    "termius--mobile-tech-lead-architect-ios-android",
]


def run_reconciliation(*, dry_run: bool = True) -> int:
    print(f"=== WITHDRAWN Records Reconciliation (dry_run={dry_run}) ===")

    # 1. Take a full snapshot backup before making any changes
    print("\n[Step 1] Creating pre-reconciliation backup...")
    backup_file = backup_firestore.backup()
    print(f"  Backup saved to: {backup_file}")

    db = client.getDb()

    # 2. Verify existence of records
    print("\n[Step 2] Verifying records in Firestore...")
    ebay_ref = db.collection(client.APPLICATIONS).document(EBAY_APP_ID)
    ebay_doc = ebay_ref.get()
    if not ebay_doc.exists:
        print(f"  [ERROR] eBay application document {EBAY_APP_ID} not found in Firestore!", file=sys.stderr)
        return 1
    print(f"  Found {EBAY_APP_ID} (current status: {ebay_doc.to_dict().get('status')})")

    to_delete_refs = []
    for aid in DELETE_APP_IDS:
        doc_ref = db.collection(client.APPLICATIONS).document(aid)
        if doc_ref.get().exists:
            to_delete_refs.append((aid, doc_ref))
            print(f"  Found {aid} for deletion")
        else:
            print(f"  [warn] {aid} already absent from Firestore")

    # 3. Prepare eBay update
    print("\n[Step 3] Preparing eBay update to DRAFTING...")
    ebay_data = ebay_doc.to_dict()
    timestamp = events_module.nowIso()

    ebay_update = {
        "status": "DRAFTING",
        "statusRank": 1,
        "isOpen": True,
        "updatedAt": timestamp,
    }

    # Event for eBay status transition
    status_event = events_module.buildEvent(
        "TAILOR_DOCUMENT",
        date=timestamp[:10],
        note="Application drafted with tailored CV and Cover Letter; status corrected to DRAFTING.",
        actor="USER",
        derivedFrom="MANUAL",
        confidence="HIGH_CONFIDENCE",
        statusFrom=ebay_data.get("status"),
        statusTo="DRAFTING",
    )

    # Artifact definitions if local files exist
    artifacts_to_add = []
    if EBAY_ARTIFACTS_DIR.exists():
        cv_pdf = EBAY_ARTIFACTS_DIR / "main_ebay_staff_android_engineer_mts_2.pdf"
        cl_pdf = EBAY_ARTIFACTS_DIR / "cover_ebay_staff_android_engineer_mts_2.pdf"
        outcome_md = EBAY_ARTIFACTS_DIR / "outcome.md"

        if cv_pdf.exists():
            artifacts_to_add.append({
                "id": "cv",
                "kind": "CV",
                "filename": cv_pdf.name,
                "localPath": str(cv_pdf.relative_to(REPO_ROOT)),
                "createdAt": timestamp,
            })
        if cl_pdf.exists():
            artifacts_to_add.append({
                "id": "cover-letter",
                "kind": "COVER_LETTER",
                "filename": cl_pdf.name,
                "localPath": str(cl_pdf.relative_to(REPO_ROOT)),
                "createdAt": timestamp,
            })
        if outcome_md.exists():
            artifacts_to_add.append({
                "id": "outcome",
                "kind": "OUTCOME",
                "filename": outcome_md.name,
                "localPath": str(outcome_md.relative_to(REPO_ROOT)),
                "createdAt": timestamp,
            })

    print(f"  eBay updates: {ebay_update}")
    print(f"  Status event: {status_event['type']} (statusTo={status_event.get('statusTo')})")
    print(f"  Artifacts to register: {[a['id'] for a in artifacts_to_add]}")

    print(f"\n[Step 4] Deletions planned: {len(to_delete_refs)} documents from 'applications'")
    for aid, _ in to_delete_refs:
        print(f"  - DELETE applications/{aid}")

    if dry_run:
        print("\n[DRY RUN COMPLETE] No writes executed. Rerun with --apply to commit changes.")
        return 0

    # 4. Execute writes
    print("\n[Step 5] Executing writes to Firestore...")
    client.assertWritesAllowed("reconcile withdrawn applications")

    batch = db.batch()

    # Apply eBay update
    batch.update(ebay_ref, ebay_update)

    # Add event to subcollection
    event_id = status_event.get("eventId") or status_event.get("id") or contract.newUuid()
    event_ref = ebay_ref.collection(client.EVENTS).document(event_id)
    batch.set(event_ref, status_event)

    # Add artifacts to subcollection
    for art in artifacts_to_add:
        art_ref = ebay_ref.collection(client.ARTIFACTS).document(art["id"])
        batch.set(art_ref, art)

    # Delete the 9 non-application records
    for aid, doc_ref in to_delete_refs:
        # Also delete any subcollection docs if they exist
        for ev in doc_ref.collection(client.EVENTS).stream():
            batch.delete(ev.reference)
        for art in doc_ref.collection(client.ARTIFACTS).stream():
            batch.delete(art.reference)
        batch.delete(doc_ref)

    batch.commit()
    print("  [SUCCESS] All batch operations committed successfully!")
    print(f"  Updated: {EBAY_APP_ID} -> DRAFTING")
    print(f"  Deleted: {len(to_delete_refs)} invalid applications")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Reconcile invalid WITHDRAWN records in Firestore")
    parser.add_argument("--apply", action="store_true", help="Execute the changes (default: dry run)")
    args = parser.parse_args()

    return run_reconciliation(dry_run=not args.apply)


if __name__ == "__main__":
    sys.exit(main())
