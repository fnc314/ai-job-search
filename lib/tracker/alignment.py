"""Repeatable alignment protocol for job search tracker records.

Aligns entries in Firebase to canonical document IDs, recalculates fit,
and updates running documentation (including outcome.md and cache).
"""

from __future__ import annotations

import datetime as dt
import json
import os
import pathlib
import re
import sys

import cache
import client
import contract
import events as eventsModule
import fit_evaluator
import render


def _format_location_str(location: dict | str | None) -> str:
    if not location:
        return "Not specified"
    if isinstance(location, str):
        return location
    parts = []
    if location.get("city"):
        parts.append(location["city"])
    if location.get("region"):
        parts.append(location["region"])
    mode = location.get("workMode", "")
    if mode:
        if mode.startswith("work-mode:"):
            mode = mode[len("work-mode:"):]
        parts.append(f"({mode})")
    return ", ".join(parts) if parts else "Not specified"


def _clean_status_label(status: str | None) -> str:
    if not status:
        return "applied"
    if status.startswith("status:"):
        return status[len("status:"):]
    return status


def update_running_documentation(
    application: dict,
    events: list[dict] | None = None,
    repo_root: pathlib.Path | None = None,
    dry_run: bool = False,
) -> pathlib.Path | None:
    """Ensure job-applications/<company>/<role>/<date>/outcome.md reflects current state."""
    repo_root = repo_root or pathlib.Path(__file__).resolve().parent.parent
    company = application.get("company") or ""
    role = application.get("role") or ""
    company_slug = contract.slug(company)
    role_slug = contract.slug(role) if role else "general"

    if not company_slug:
        return None

    company_dir = repo_root / "job-applications" / company_slug
    if not company_dir.exists():
        if not dry_run:
            company_dir.mkdir(parents=True, exist_ok=True)

    target_role_dir = company_dir / role_slug

    # If role was renamed or refined, check if an existing folder should be migrated
    if not target_role_dir.exists() and company_dir.exists():
        # Look for existing single role subfolder or partial match
        existing_subdirs = [d for d in company_dir.iterdir() if d.is_dir() and not d.name.startswith(".")]
        if len(existing_subdirs) == 1 and existing_subdirs[0].name != role_slug:
            # If the only other subdir was an older slug for this role, rename it
            old_dir = existing_subdirs[0]
            sys.stderr.write(f"Migrating archive folder from {old_dir.name} to {role_slug}\n")
            if not dry_run:
                old_dir.rename(target_role_dir)

    # Determine date folder
    date_val = application.get("date") or eventsModule.nowIso()
    if isinstance(date_val, dt.datetime):
        date_str = date_val.strftime("%Y-%m-%d")
    else:
        date_str = str(date_val)[:10]

    # Find existing date dir or use target
    dated_dir = target_role_dir / date_str
    existing_outcome = None
    if target_role_dir.exists():
        date_subdirs = [d for d in target_role_dir.iterdir() if d.is_dir() and not d.name.startswith(".")]
        if date_subdirs:
            # Use most recent existing dated dir
            date_subdirs.sort(key=lambda d: d.name, reverse=True)
            dated_dir = date_subdirs[0]
            candidate_file = dated_dir / "outcome.md"
            if candidate_file.exists():
                existing_outcome = candidate_file

    outcome_path = existing_outcome or (dated_dir / "outcome.md")

    # Read existing content if present
    existing_text = ""
    if outcome_path.exists():
        try:
            existing_text = outcome_path.read_text(encoding="utf-8")
        except Exception:
            existing_text = ""

    # Build sections
    status_str = _clean_status_label(application.get("status"))
    fit_rating = application.get("fitRating")
    job_url = application.get("jobUrl") or application.get("source") or "Not disclosed"
    app_url = application.get("jobApplicationUrl") or job_url
    contact_person = application.get("contactPerson")
    contact_email = application.get("contactEmail")
    location_str = _format_location_str(application.get("location"))

    contact_str = ""
    if contact_person and contact_email:
        contact_str = f"{contact_person} ({contact_email})"
    elif contact_person:
        contact_str = contact_person
    elif contact_email:
        contact_str = contact_email

    # Extract interview stages and notes from events
    all_events = list(events or [])
    all_events.sort(key=lambda e: (str(e.get("date") or "")[:10], str(e.get("ts") or "")), reverse=False)

    recruiter_screen_done = False
    recruiter_screen_info = ""
    tech_screen_done = False
    tech_screen_info = ""
    offer_done = False

    notes_lines = []
    for ev in all_events:
        ev_type = ev.get("type", "")
        ev_date = str(ev.get("date") or "")[:10]
        ev_note = ev.get("note") or ""
        if "recruiter-screen" in ev_type:
            recruiter_screen_done = True
            recruiter_screen_info = f"Date: {ev_date}" + (f" - {ev_note}" if ev_note else "")
        elif "interview" in ev_type or "technical-screen" in ev_type:
            tech_screen_done = True
            tech_screen_info = f"Date: {ev_date}" + (f" - {ev_note}" if ev_note else "")
        elif "offer" in ev_type:
            offer_done = True

        if ev_note:
            notes_lines.append(f"- {ev_date}: {ev_note}")

    if not notes_lines and application.get("notesRendered"):
        for line in str(application["notesRendered"]).split("\n"):
            line = line.strip()
            if line:
                notes_lines.append(f"- {line}")

    # Build Header
    lines = [
        f"# Job Application: {company} — {role}",
        "",
        f"**Date:** {date_str}",
        f"**Status:** {status_str}",
        f"**Fit Rating:** {fit_rating if fit_rating is not None else 'Pending'}",
        f"**URL:** {job_url}",
    ]
    if app_url and app_url != job_url and app_url != "Not disclosed":
        lines.append(f"**Application URL:** {app_url}")
    lines.append(f"**Location:** {location_str}")
    if contact_str:
        lines.append(f"**Contact:** {contact_str}")
    lines.append("")

    # Application Process section
    lines.extend([
        "## Application Process",
        "",
        f"- [x] Status: {status_str.capitalize()}",
        f"- [x] Resume submitted (Date: {date_str})",
        f"- [{'x' if recruiter_screen_done else ' '}] Recruiter screen ({recruiter_screen_info})",
        f"- [{'x' if tech_screen_done else ' '}] Technical screen ({tech_screen_info})",
        f"- [{'x' if offer_done else ' '}] Offer received",
        "",
    ])

    # Notes section
    lines.append("## Notes")
    if notes_lines:
        lines.extend(notes_lines)
    else:
        lines.append("- Initial record created.")
    lines.append("")

    # Fit Evaluation section (preserve existing or append newly evaluated)
    fit_eval_match = re.search(r"## Initial Fit Evaluation[\s\S]*", existing_text)
    if fit_eval_match:
        lines.append(fit_eval_match.group(0).strip())
        lines.append("")
    else:
        eval_result = fit_evaluator.evaluate_fit(application)
        lines.extend([
            "## Initial Fit Evaluation",
            "",
            "### 1. Skills Match",
        ])
        for sm in eval_result["skillsMatch"]:
            lines.append(f"- {sm}")
        if eval_result.get("gaps"):
            for gap in eval_result["gaps"]:
                lines.append(f"- Minor Gap: {gap}")
        lines.extend([
            "",
            "### 2. Experience Match",
        ])
        for em in eval_result["experienceMatch"]:
            lines.append(f"- {em}")
        lines.extend([
            "",
            "### 3. Behavioral / Culture Match",
        ])
        for bm in eval_result["behavioralMatch"]:
            lines.append(f"- {bm}")
        lines.extend([
            "",
            "### 4. Salary Benchmark",
            f"- {eval_result['salaryBenchmark']}",
            "",
            "### 5. Overall Fit Score & Recommendation",
            f"- **Overall Fit Score:** {eval_result['score']} / 100",
            f"- **Recommendation:** {eval_result['recommendation']}",
            "",
        ])

    new_content = "\n".join(lines).strip() + "\n"

    if not dry_run:
        dated_dir.mkdir(parents=True, exist_ok=True)
        outcome_path.write_text(new_content, encoding="utf-8")
        sys.stderr.write(f"Updated running documentation: {outcome_path}\n")

    return outcome_path


def align_application(
    db,
    identifier_or_ref,
    updated_doc: dict | None = None,
    dry_run: bool = False,
    recalc_fit: bool = True,
    update_docs: bool = True,
    note: str | None = None,
) -> dict:
    """Align a single application entry in Firestore and filesystem.

    - Derives canonical id: contract.documentId(company, role)
    - Derives canonical companyId: contract.slug(company)
    - If doc key in Firestore != canonical id: migrates doc and all subcollections
    - Recalculates fit if requested
    - Updates outcome.md and running documentation
    - Refreshes local read cache
    """
    ref = None
    existing = None

    if hasattr(identifier_or_ref, "get"):
        ref = identifier_or_ref
        snap = ref.get()
        if not snap.exists:
            raise contract.ContractError(f"Application does not exist at reference: {ref.id}")
        existing = contract.fromFirestore(snap.to_dict())
    else:
        from tracker import findApplication
        existing, ref = findApplication(db, str(identifier_or_ref))
        if not existing:
            raise contract.ContractError(f"Application not found: {identifier_or_ref}")

    doc = dict(updated_doc or existing)

    # Derive canonical identifiers
    company = doc.get("company")
    if not company:
        raise contract.ContractError("Application is missing required vital field 'company'")
    role = doc.get("role")
    canonical_id = contract.documentId(company, role)
    canonical_company_id = contract.slug(company)

    current_key = ref.id if ref else doc.get("id")
    needs_key_migration = (ref is not None and ref.id != canonical_id)
    needs_id_update = (doc.get("id") != canonical_id or doc.get("companyId") != canonical_company_id)

    # Read events early for fit / timeline / docs
    existing_events = []
    if ref is not None:
        try:
            for subdoc in ref.collection(client.EVENTS).stream():
                existing_events.append(contract.fromFirestore(subdoc.to_dict()))
        except Exception as e:
            sys.stderr.write(f"Warning: could not read existing events: {e}\n")

    # Fit Recalculation
    fit_event = None
    old_fit = doc.get("fitRating")
    if recalc_fit:
        eval_result = fit_evaluator.evaluate_fit(doc)
        new_fit = eval_result["score"]
        if old_fit is None or (old_fit != new_fit and (updated_doc or needs_id_update)):
            doc["fitRating"] = new_fit
            fit_event = eventsModule.buildEvent(
                "event:fit-recalculated",
                date=eventsModule.nowIso()[:10],
                note=f"Recalculated fit: {eval_result['recommendation']}",
                actor="actor:user",
                derivedFrom="manual",
                confidence="high",
                metadata={"fitRating": new_fit, "previousFitRating": old_fit},
            )
            existing_events.append(fit_event)

    now = eventsModule.nowIso()
    doc["id"] = canonical_id
    doc["companyId"] = canonical_company_id
    doc["updatedAt"] = now
    doc["schemaVersion"] = contract.schemaVersion()
    if not doc.get("createdAt"):
        doc["createdAt"] = doc.get("date") or now
    if not doc.get("uuid"):
        doc["uuid"] = contract.newUuid()

    # Quarantine unexpected top-level properties into metadata
    allowed_props = set(contract.definition("application")["properties"].keys())
    for k in list(doc.keys()):
        if k not in allowed_props:
            if "metadata" not in doc or not isinstance(doc.get("metadata"), dict):
                doc["metadata"] = {}
            doc["metadata"][k] = doc.pop(k)

    contract.validate("application", doc)

    # Audit event for the alignment or edit
    audit_event = None
    if needs_key_migration or needs_id_update or note:
        audit_note = note or f"Data alignment: aligned document id to {canonical_id}"
        audit_event = eventsModule.buildEvent(
            "event:note-added",
            date=now[:10],
            note=audit_note,
            actor="actor:user",
            derivedFrom="manual",
            confidence="high",
            metadata={"alignedId": canonical_id, "previousDocKey": current_key},
        )
        existing_events.append(audit_event)

    # Update running documentation
    outcome_file = None
    if update_docs:
        outcome_file = update_running_documentation(doc, existing_events, dry_run=dry_run)

    if dry_run:
        return {
            "dryRun": True,
            "oldKey": current_key,
            "canonicalId": canonical_id,
            "needsKeyMigration": needs_key_migration,
            "fitRating": doc.get("fitRating"),
            "previousFitRating": old_fit,
            "outcomeFile": str(outcome_file) if outcome_file else None,
            "doc": doc,
        }

    client.assertWritesAllowed("an application data alignment")

    target_ref = db.collection(client.APPLICATIONS).document(canonical_id)

    # Subcollection migration if Firestore key moved
    if needs_key_migration:
        sys.stderr.write(f"Migrating Firestore entry from '{current_key}' to '{canonical_id}'...\n")

        # Discover all subcollections under the old reference
        subcollections_data = {}
        for coll in ref.collections():
            coll_name = coll.id
            subcollections_data[coll_name] = []
            for subdoc in coll.stream():
                subcollections_data[coll_name].append((subdoc.id, subdoc.to_dict()))

        # Write new document and copy all subcollections
        batch = db.batch()
        batch.set(target_ref, contract.toFirestore(doc))

        for coll_name, subdocs in subcollections_data.items():
            for subdoc_id, subdoc_data in subdocs:
                batch.set(target_ref.collection(coll_name).document(subdoc_id), subdoc_data)

        if fit_event:
            batch.set(target_ref.collection(client.EVENTS).document(fit_event["eventId"]), contract.toFirestore(fit_event))
        if audit_event:
            batch.set(target_ref.collection(client.EVENTS).document(audit_event["eventId"]), contract.toFirestore(audit_event))

        batch.commit()

        # Delete old subcollections and old document
        delete_batch = db.batch()
        for coll_name, subdocs in subcollections_data.items():
            for subdoc_id, _ in subdocs:
                delete_batch.delete(ref.collection(coll_name).document(subdoc_id))
        delete_batch.delete(ref)
        delete_batch.commit()
        sys.stderr.write(f"Successfully migrated Firestore document '{current_key}' -> '{canonical_id}'.\n")

    else:
        # Key was already canonical; update document and append new events
        batch = db.batch()
        batch.set(target_ref, contract.toFirestore(doc), merge=True)
        if fit_event:
            batch.set(target_ref.collection(client.EVENTS).document(fit_event["eventId"]), contract.toFirestore(fit_event))
        if audit_event:
            batch.set(target_ref.collection(client.EVENTS).document(audit_event["eventId"]), contract.toFirestore(audit_event))
        batch.commit()

    # Refresh read cache
    try:
        cache.refresh(db)
    except Exception as e:
        sys.stderr.write(f"Warning: could not refresh cache: {e}\n")

    return {
        "ok": True,
        "id": canonical_id,
        "previousKey": current_key,
        "migrated": needs_key_migration,
        "fitRating": doc.get("fitRating"),
        "previousFitRating": old_fit,
        "outcomeFile": str(outcome_file) if outcome_file else None,
    }


def align_all_applications(db, dry_run: bool = False) -> list[dict]:
    """Scan all applications in Firestore and align misaligned keys/fields."""
    results = []
    for snapshot in db.collection(client.APPLICATIONS).stream():
        data = contract.fromFirestore(snapshot.to_dict())
        company = data.get("company")
        if not company:
            continue
        role = data.get("role")
        canonical_id = contract.documentId(company, role)
        canonical_cid = contract.slug(company)

        needs_align = (snapshot.id != canonical_id or data.get("id") != canonical_id or data.get("companyId") != canonical_cid)
        if needs_align:
            sys.stderr.write(f"Aligning misaligned entry: '{snapshot.id}' -> '{canonical_id}'\n")
            res = align_application(
                db,
                snapshot.reference,
                dry_run=dry_run,
                recalc_fit=True,
                update_docs=True,
            )
            results.append(res)

    return results
