"""DEPRECATED: As part of RM-001/RM-003, projection logic has moved to Kotlin
records/parser (ApplicationTimelineProjector & EventNoteRenderer).
This module is in Phase 1 of retirement and will be removed in Phase 3.

Derived, read-only projections of the event log.

`notesRendered` is output, not input. It is regenerated from events so that the
human-readable history can never drift from the recorded one, and so a UI never
has to parse prose to find out what happened.

The functions here are pure: given events, they return fields. Nothing writes.
"""

from __future__ import annotations

import datetime as dt

import contract
import events as eventsModule

# Which events increment which counter. Keeping this explicit means a new event
# type cannot silently start contributing to a total.
COUNTER_RULES = {
    "event:follow-up-sent": "followUpsSent",
    "event:interview-scheduled": "interviews",
    "event:onsite": "interviews",
    "event:rejection": "rejections",
    "SCHEDULE_INTERVIEW": "interviews",
    "VISIT_ONSITE": "interviews",
    "REJECT": "rejections",
}

NOTE_SEPARATOR = " · "
NOTE_DASH = " — "


def parseDate(value: str | None) -> dt.datetime | None:
    if not value:
        return None
    try:
        parsed = dt.datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError:
        return None
    # A naive date is treated as UTC midnight purely for day arithmetic. It is
    # never rendered, so no calendar drift can reach the output.
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=dt.timezone.utc)
    return parsed


def renderNote(event: dict) -> str:
    """One line per event: `<date> · <type> — <note>`.

    `event:imported` is the exception. It is a provenance marker wrapping the
    pre-migration prose, not an activity that happened on a day, so decorating it
    with a date and a type would prepend `2026-10-30 · event:imported — ` to all
    84 restored records while adding no information. Post-import events keep the
    full form, so real history is still dated and typed.
    """
    note = event.get("note")
    if event.get("type") == "event:imported":
        return note or ""
    date = str(event.get("date") or "")[:10]
    line = f"{date}{NOTE_SEPARATOR}{event.get('type', 'event:note-added')}"
    if note:
        line += f"{NOTE_DASH}{note}"
    return line


def renderNotes(events: list[dict]) -> str | None:
    """The full human-readable history, oldest first. None when there is none."""
    ordered = eventsModule.chronologically(events)
    lines = [line for line in (renderNote(event) for event in ordered) if line]
    if not lines:
        return None
    return "\n".join(lines)


def counters(events: list[dict]) -> dict:
    """Counts derived from the log. `events` is the log length, not a stored total."""
    result = {"events": 0, "followUpsSent": 0, "interviews": 0, "rejections": 0}
    for event in events:
        result["events"] += 1
        target = COUNTER_RULES.get(event.get("type"))
        if target:
            result[target] += 1
    return result


def timeline(events: list[dict], today: dt.date | None = None) -> dict:
    """First/last activity, submittedAt, submissionConfirmedAt, and days since."""
    moments = [parseDate(event.get("date")) for event in events]
    moments = [moment for moment in moments if moment is not None]
    if not moments:
        return {
            "firstActivityAt": None,
            "lastActivityAt": None,
            "daysSinceActivity": None,
            "nextFollowUpAt": None,
            "submittedAt": None,
            "submissionConfirmedAt": None,
        }
    first = min(moments)
    last = max(moments)
    reference = today or dt.date.today()

    submitted_at = None
    submission_confirmed_at = None

    # Derive submittedAt from earliest APPLY / event:applied event
    apply_events = [ev for ev in events if ev.get("type") in ("APPLY", "event:applied")]
    if apply_events:
        apply_dates = [parseDate(ev.get("date")) for ev in apply_events]
        valid_apply_dates = [d for d in apply_dates if d is not None]
        if valid_apply_dates:
            submitted_at = min(valid_apply_dates).isoformat()

    # Derive submissionConfirmedAt from earliest ACKNOWLEDGE / event:acknowledged event
    ack_events = [ev for ev in events if ev.get("type") in ("ACKNOWLEDGE", "event:acknowledged")]
    if ack_events:
        ack_dates = [parseDate(ev.get("date")) for ev in ack_events]
        valid_ack_dates = [d for d in ack_dates if d is not None]
        if valid_ack_dates:
            submission_confirmed_at = min(valid_ack_dates).isoformat()

    return {
        "firstActivityAt": first.isoformat(),
        "lastActivityAt": last.isoformat(),
        "daysSinceActivity": max(0, (reference - last.date()).days),
        "nextFollowUpAt": None,
        "submittedAt": submitted_at,
        "submissionConfirmedAt": submission_confirmed_at,
    }


def project(application: dict, events: list[dict]) -> dict:
    """Return the stored read-only fields, recomputed from the log.

    Status fields are included because a status-bearing event is the authority
    on the funnel position: if someone appends `event:rejection`, the derived
    `status`/`isOpen` must follow, or the record lies.
    """
    tl = timeline(events)
    projected = {
        "notesRendered": renderNotes(events),
        "counters": counters(events),
        "timeline": tl,
    }

    # Extract email message IDs
    email_ids = list(application.get("emailMessageIds") or [])
    for ev in events:
        mid = ev.get("emailMessageId")
        if mid and mid not in email_ids:
            email_ids.append(mid)
    if email_ids:
        projected["emailMessageIds"] = sorted(set(email_ids))

    # Reconcile submissionConfirmation
    existing_conf = application.get("submissionConfirmation") or {}
    ack_events = [ev for ev in events if ev.get("type") in ("ACKNOWLEDGE", "event:acknowledged")]
    apply_events = [ev for ev in events if ev.get("type") in ("APPLY", "event:applied")]

    # If already set on application (e.g. via backfill or recruiter led), preserve unless ack added
    if ack_events:
        earliest_ack = min(ack_events, key=lambda ev: ev.get("date") or "")
        projected["submissionConfirmation"] = {
            "isConfirmed": True,
            "confidence": "CONFIRMED_SCRAPED",
            "confirmedAt": tl["submissionConfirmedAt"] or earliest_ack.get("date"),
            "confirmationEmailMessageId": earliest_ack.get("emailMessageId") or existing_conf.get("confirmationEmailMessageId"),
            "sourceSubject": earliest_ack.get("actor", "USER"),
            "notes": earliest_ack.get("note") or "Employer confirmation email scraped and matched",
        }
    elif existing_conf.get("isConfirmed"):
        projected["submissionConfirmation"] = existing_conf
    elif apply_events:
        earliest_apply = min(apply_events, key=lambda ev: ev.get("date") or "")
        projected["submissionConfirmation"] = {
            "isConfirmed": False,
            "confidence": "UNCONFIRMED_USER_MARKED",
            "confirmedAt": None,
            "confirmationEmailMessageId": None,
            "sourceSubject": earliest_apply.get("actor", "USER"),
            "notes": "User marked application as submitted without employer acknowledgment email",
        }

    implied = eventsModule.latestStatus(events)
    current = application.get("status")
    if implied and current != implied:
        projected.update(contract.deriveStatusFields(implied))
    elif current:
        projected.update(contract.deriveStatusFields(current))
    return projected
