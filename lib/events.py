"""Append-only event history.

Notes are discouraged because prose is unqueryable and lossy. The replacement is
one immutable event per thing that happened. Nothing in this module edits or
deletes an event: a correction is a new event of type `event:status-corrected`.

Two rules live here and nowhere else:

1. An event is built and validated as a whole, so a caller cannot write a
   half-formed history entry.
2. Provenance is mandatory. Migrated or inferred history says so, via
   `derivedFrom` and `confidence`, and never presents as recorded fact.
"""

from __future__ import annotations

import datetime as dt
import hashlib
from typing import Iterable

import client
import contract

GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

# Event types that imply a funnel status. An event carries statusFrom/statusTo
# only when it actually moved the funnel; a follow-up does not.
EVENT_IMPLIED_STATUS = {
    "APPLY": "APPLIED",
    "RECRUITER_SCREEN": "INTERVIEW",
    "SCHEDULE_INTERVIEW": "INTERVIEW",
    "VISIT_ONSITE": "INTERVIEW",
    "OFFER": "OFFER_EXPECTED",
    "ACCEPT_OFFER": "HIRED",
    "DECLINE_OFFER": "OFFER_DECLINED",
    "REJECT": "REJECTED",
    "GHOST": "NO_RESPONSE",
    "WITHDRAW": "APPLICATION_WITHDRAWN",
    # Legacy support
    "event:applied": "APPLIED",
    "event:recruiter-screen": "INTERVIEW",
    "event:interview-scheduled": "INTERVIEW",
    "event:onsite": "INTERVIEW",
    "event:offer": "OFFER_EXPECTED",
    "event:offer-accepted": "HIRED",
    "event:offer-declined": "OFFER_DECLINED",
    "event:rejection": "REJECTED",
    "event:no-response": "NO_RESPONSE",
    "event:withdrawn": "APPLICATION_WITHDRAWN",
}

# Event types that must never silently move the funnel: they describe activity
# inside the current status, or describe the record itself.
NON_STATUS_EVENTS = {
    "ACKNOWLEDGE",
    "event:acknowledged",
    "event:interview-completed",
    "event:assessment",
    "event:follow-up-sent",
    "event:document-tailored",
    "event:fit-recalculated",
    "event:artifact-added",
    "event:note-added",
    "event:imported",
    "CORRECT_STATUS",
}


class EventError(ValueError):
    """An event cannot be recorded as requested."""


def computeEventHash(
    sequenceNumber: int,
    prevEventHash: str,
    eventId: str,
    applicationId: str,
    eventType: str,
    timestampIso: str,
    statusTo: str | None,
    actor: str,
) -> str:
    """Computes SHA-256 hash chaining this event to its predecessor."""
    payload = f"{sequenceNumber}|{prevEventHash}|{eventId}|{applicationId}|{eventType}|{timestampIso}|{statusTo or ''}|{actor}"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def chainEvents(events: Iterable[dict], applicationId: str) -> list[dict]:
    """Takes events, sorts them chronologically, and assigns sequential hash links."""
    ordered = chronologically(events)
    expected_prev = GENESIS_PREV_HASH
    chained: list[dict] = []
    for idx, ev in enumerate(ordered, start=1):
        ev_copy = dict(ev)
        ev_copy["sequenceNumber"] = idx
        ev_copy["prevEventHash"] = expected_prev
        ts_iso = ev_copy.get("ts") or ev_copy.get("date") or ""
        ev_copy["eventHash"] = computeEventHash(
            sequenceNumber=idx,
            prevEventHash=expected_prev,
            eventId=ev_copy.get("eventId", ""),
            applicationId=applicationId,
            eventType=ev_copy.get("type", ""),
            timestampIso=ts_iso,
            statusTo=ev_copy.get("statusTo"),
            actor=ev_copy.get("actor", "USER"),
        )
        expected_prev = ev_copy["eventHash"]
        chained.append(ev_copy)
    return chained


def verifyEventChain(events: list[dict], applicationId: str) -> bool:
    """Verifies that an event list forms an unbroken cryptographic hash chain."""
    if not events:
        return True
    expected_prev = GENESIS_PREV_HASH
    for idx, ev in enumerate(events, start=1):
        if ev.get("sequenceNumber") != idx:
            return False
        if ev.get("prevEventHash") != expected_prev:
            return False
        ts_iso = ev.get("ts") or ev.get("date") or ""
        computed = computeEventHash(
            sequenceNumber=idx,
            prevEventHash=expected_prev,
            eventId=ev.get("eventId", ""),
            applicationId=applicationId,
            eventType=ev.get("type", ""),
            timestampIso=ts_iso,
            statusTo=ev.get("statusTo"),
            actor=ev.get("actor", "USER"),
        )
        if ev.get("eventHash") != computed:
            return False
        expected_prev = computed
    return True


def impliedStatus(eventType: str) -> str | None:
    """The status an event type implies, or None if it does not move the funnel."""
    return EVENT_IMPLIED_STATUS.get(eventType)


def statusEventType(status: str) -> str:
    """The canonical event type for entering a status."""
    normalized = contract.normalizeStatus(status)
    for eventType, implied in EVENT_IMPLIED_STATUS.items():
        if implied == normalized:
            return eventType
    # status:drafting has no inbound event type of its own; it is the initial
    # state of a record, so entering it is a correction rather than progress.
    return "CORRECT_STATUS"


def nowIso() -> str:
    """Local-time ISO 8601 with an explicit offset. Never a naive timestamp."""
    return dt.datetime.now().astimezone().isoformat(timespec="seconds")


def normalizeDate(value: str | None) -> str:
    """Accept a bare date or a full timestamp and return a legal contract value."""
    if value is None:
        return nowIso()
    text = value.strip()
    if not text:
        raise EventError("date is empty")
    try:
        parsed = dt.datetime.fromisoformat(text.replace("Z", "+00:00"))
    except ValueError:
        raise EventError(f"date is not ISO 8601: {value!r}") from None
    if "T" not in text:
        # A bare date is a calendar fact, not an instant. Keep it naive so no
        # reader west of UTC sees the previous day.
        return parsed.date().isoformat()
    return parsed.isoformat()


def buildEvent(
    eventType: str,
    *,
    date: str | None = None,
    note: str | None = None,
    actor: str = "actor:user",
    derivedFrom: str = "manual",
    confidence: str = "high",
    statusFrom: str | None = None,
    statusTo: str | None = None,
    emailMessageId: str | None = None,
    metadata: dict | None = None,
    sequenceNumber: int | None = None,
    prevEventHash: str | None = None,
    eventHash: str | None = None,
) -> dict:
    """Construct and validate one event document.

    `statusTo` defaults to the status implied by the event type, and `statusFrom`
    to nothing: an event describes what it moved *to*, and callers that know the
    prior state pass it. Guessing the prior state here would invent history.
    """
    if actor.startswith("actor:"):
        actor = actor.split(":", 1)[1].upper()
    elif actor.lower() in ("user", "agent", "email_sync", "import", "scheduler"):
        actor = actor.upper()

    if derivedFrom.startswith("derived-from:") or derivedFrom.startswith("derivedFrom:"):
        derivedFrom = derivedFrom.split(":", 1)[1]
    derivedFromMap = {"manual": "MANUAL", "notes_regex": "NOTES_REGEX", "notes-regex": "NOTES_REGEX", "email": "EMAIL", "csv_import": "CSV_IMPORT", "csv-import": "CSV_IMPORT"}
    derivedFrom = derivedFromMap.get(derivedFrom.lower(), derivedFrom.upper())

    if confidence.startswith("confidence:"):
        confidence = confidence.split(":", 1)[1]
    confidenceMap = {"high": "HIGH_CONFIDENCE", "medium": "MEDIUM_CONFIDENCE", "low": "LOW_CONFIDENCE"}
    confidence = confidenceMap.get(confidence.lower(), confidence.upper())

    if eventType not in contract.enums("eventType"):
        raise EventError(
            f"unknown event type {eventType!r}. Known types: "
            f"{', '.join(contract.enums('eventType'))}"
        )
    if actor not in contract.enums("actor"):
        raise EventError(f"unknown actor {actor!r}")
    if derivedFrom not in contract.enums("derivedFrom"):
        raise EventError(f"unknown derivedFrom {derivedFrom!r}")
    if confidence not in contract.enums("confidence"):
        raise EventError(f"unknown confidence {confidence!r}")

    resolvedDate = normalizeDate(date)
    resolvedType = eventType
    if statusTo is None:
        statusTo = impliedStatus(eventType)
    else:
        statusTo = contract.normalizeStatus(statusTo)
    if statusFrom is not None:
        statusFrom = contract.normalizeStatus(statusFrom)

    event = {
        "eventId": contract.newUuid(),
        "ts": nowIso(),
        "date": resolvedDate,
        "type": resolvedType,
        "actor": actor,
        "derivedFrom": derivedFrom,
        "confidence": confidence,
        "statusFrom": statusFrom,
        "statusTo": statusTo,
        "note": note or None,
        "emailMessageId": emailMessageId or None,
        "metadata": metadata or {},
    }
    if sequenceNumber is not None:
        event["sequenceNumber"] = sequenceNumber
    if prevEventHash is not None:
        event["prevEventHash"] = prevEventHash
    if eventHash is not None:
        event["eventHash"] = eventHash

    return contract.validate("event", event)


def appendEvent(db, applicationId: str, event: dict) -> dict:
    """Write one validated event. Callers must have passed the write gate.
    
    If sequenceNumber or eventHash is missing, automatically computes next hash chain link.
    """
    if "sequenceNumber" not in event or "eventHash" not in event:
        existing = readEvents(db, applicationId)
        ordered = chronologically(existing)
        seq = len(ordered) + 1
        prev_hash = ordered[-1].get("eventHash", GENESIS_PREV_HASH) if ordered else GENESIS_PREV_HASH
        event["sequenceNumber"] = seq
        event["prevEventHash"] = prev_hash
        ts_iso = event.get("ts") or event.get("date") or ""
        event["eventHash"] = computeEventHash(
            sequenceNumber=seq,
            prevEventHash=prev_hash,
            eventId=event.get("eventId", ""),
            applicationId=applicationId,
            eventType=event.get("type", ""),
            timestampIso=ts_iso,
            statusTo=event.get("statusTo"),
            actor=event.get("actor", "USER"),
        )

    validated = contract.validate("event", event)
    reference = (
        db.collection(client.APPLICATIONS)
        .document(applicationId)
        .collection(client.EVENTS)
        .document(validated["eventId"])
    )
    reference.set(contract.toFirestore(validated))
    return validated


def readEvents(db, applicationId: str) -> list[dict]:
    """Events in chronological order."""
    collection = (
        db.collection(client.APPLICATIONS)
        .document(applicationId)
        .collection(client.EVENTS)
    )
    raw_events = [contract.fromFirestore(doc.to_dict()) for doc in collection.stream()]
    return chronologically(raw_events)


def chronologically(events: Iterable[dict]) -> list[dict]:
    """Sort by sequenceNumber if available, else date, then by UUIDv7 as a stable tiebreak."""

    def key(event):
        seq = event.get("sequenceNumber")
        date = event.get("date") or ""
        # If sequenceNumber is set, it is the primary chronological sequence
        if seq is not None:
            return (0, seq, date, event.get("eventId") or "")
        return (1, 0, date, event.get("eventId") or "")

    return sorted(events, key=key)


def latestStatus(events: Iterable[dict]) -> str | None:
    """The status implied by the last status-bearing event, if any."""
    for event in reversed(chronologically(events)):
        if event.get("statusTo"):
            return contract.normalizeStatus(event["statusTo"])
    return None
