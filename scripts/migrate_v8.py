#!/usr/bin/env python3
"""
Migration utility for Schema Version 8: Exact Enum.name Alignment.
Transforms namespaced enum values (e.g. status:applied -> APPLIED,
channel:workday -> WORKDAY, work-mode:remote -> REMOTE) across all collections.
"""

from typing import Any

STATUS_MAP = {
    "status:lead": "LEAD",
    "status:drafting": "DRAFTING",
    "status:applied": "APPLIED",
    "status:interview": "INTERVIEW",
    "status:offer": "OFFER_EXPECTED",
    "status:hired": "HIRED",
    "status:rejected": "REJECTED",
    "status:no-response": "NO_RESPONSE",
    "status:offer-declined": "OFFER_DECLINED",
    "status:withdrawn": "APPLICATION_WITHDRAWN",
}

CHANNEL_MAP = {
    "channel:greenhouse": "GREENHOUSE",
    "channel:workday": "WORKDAY",
    "channel:lever": "LEVER",
    "channel:ashby": "ASHBY",
    "channel:smartrecruiters": "SMARTRECRUITERS",
    "channel:linkedin": "LINKEDIN",
    "channel:referral": "REFERRAL",
    "channel:direct": "DIRECT",
    "channel:other": "OTHER",
    "channel:outreach:linkedin": "OUTREACH_LINKEDIN",
    "channel:outreach:email": "OUTREACH_EMAIL",
    "channel:outreach:referral": "OUTREACH_REFERRAL",
}

ROLE_LOCATION_MAP = {
    "work-mode:onsite": "ONSITE",
    "work-mode:hybrid": "HYBRID",
    "work-mode:remote": "REMOTE",
    "onsite": "ONSITE",
    "hybrid": "HYBRID",
    "remote": "REMOTE",
}

EVENT_TYPE_MAP = {
    "event:applied": "APPLY",
    "event:acknowledged": "ACKNOWLEDGE",
    "event:recruiter-screen": "RECRUITER_SCREEN",
    "event:interview-scheduled": "SCHEDULE_INTERVIEW",
    "event:interview-completed": "COMPLETE_INTERVIEW",
    "event:assessment": "ASSESS",
    "event:onsite": "VISIT_ONSITE",
    "event:offer": "OFFER",
    "event:offer-accepted": "ACCEPT_OFFER",
    "event:offer-declined": "DECLINE_OFFER",
    "event:rejection": "REJECT",
    "event:no-response": "GHOST",
    "event:withdrawn": "WITHDRAW",
    "event:follow-up-sent": "SEND_FOLLOW_UP",
    "event:document-tailored": "TAILOR_DOCUMENT",
    "event:fit-recalculated": "RECALCULATE_FIT",
    "event:artifact-added": "ADD_ARTIFACT",
    "event:note-added": "ADD_NOTE",
    "event:imported": "IMPORT",
    "event:status-corrected": "CORRECT_STATUS",
    "event:recruiter-outreach": "RECRUITER_OUTREACH",
}

ACTOR_MAP = {
    "actor:user": "USER",
    "actor:agent": "AGENT",
    "actor:gmail-sync": "EMAIL_SYNC",
    "actor:email-sync": "EMAIL_SYNC",
    "actor:import": "IMPORT",
    "actor:scheduler": "SCHEDULER",
}

ARTIFACT_KIND_MAP = {
    "artifact:cv": "CV",
    "artifact:cover-letter": "COVER_LETTER",
    "artifact:outcome": "OUTCOME",
    "artifact:posting": "POSTING",
    "artifact:assessment": "ASSESSMENT",
}

PROPOSAL_KIND_MAP = {
    "proposal:gmail-sync": "EMAIL_SYNC",
    "proposal:email-sync": "EMAIL_SYNC",
    "proposal:sheet-sync": "SHEET_SYNC",
    "proposal:lead": "LEAD",
}

PROPOSAL_STATE_MAP = {
    "proposal:pending": "PENDING",
    "proposal:approved": "APPROVED",
    "proposal:rejected": "REJECTED",
    "proposal:expired": "EXPIRED",
}

TRIGGER_MAP = {
    "trigger:cron": "CRON",
    "trigger:application-created": "APPLICATION_CREATED",
    "trigger:application-updated": "APPLICATION_UPDATED",
    "trigger:manual": "MANUAL",
}

SYNC_STATE_MAP = {
    "sync:pending": "PENDING",
    "sync:claimed": "CLAIMED",
    "sync:done": "DONE",
    "sync:failed": "FAILED",
}

CONFIDENCE_MAP = {
    "high": "HIGH_CONFIDENCE",
    "medium": "MEDIUM_CONFIDENCE",
    "low": "LOW_CONFIDENCE",
    "confidence:high": "HIGH_CONFIDENCE",
    "confidence:medium": "MEDIUM_CONFIDENCE",
    "confidence:low": "LOW_CONFIDENCE",
}

DERIVED_FROM_MAP = {
    "manual": "MANUAL",
    "notes-regex": "NOTES_REGEX",
    "gmail": "EMAIL",
    "email": "EMAIL",
    "csv-import": "CSV_IMPORT",
}

REGION_MAP = {
    "PIT": "US_PIT",
    "NYC": "US_NYC",
    "SF": "US_SF",
    "LA": "US_LA",
    "DC": "US_DC",
    "REMOTE-US": "US_REMOTE",
    "US-REMOTE": "US_REMOTE",
}

TAG_MAP = {
    "relocation:required": "RELOCATION_REQUIRED",
    "relocation:accepted": "RELOCATION_ACCEPTED",
    "relocation:declined": "RELOCATION_DECLINED",
    "compensation:above-super-commuter:NYC": "COMPENSATION_ABOVE_SUPER_COMMUTER_NYC",
    "compensation:above-super-commuter:SF": "COMPENSATION_ABOVE_SUPER_COMMUTER_SF",
    "compensation:below-nyc-floor": "COMPENSATION_BELOW_NYC_FLOOR",
    "compensation:below-band": "COMPENSATION_BELOW_BAND",
    "compensation:at-band": "COMPENSATION_AT_BAND",
    "compensation:above-band": "COMPENSATION_ABOVE_BAND",
    "compensation:equity-heavy": "COMPENSATION_EQUITY_HEAVY",
    "fit:strong": "FIT_STRONG",
    "fit:stretch": "FIT_STRETCH",
    "fit:gap-ios": "FIT_GAP_IOS",
    "fit:gap-cross-platform": "FIT_GAP_CROSS_PLATFORM",
    "process:take-home": "PROCESS_TAKE_HOME",
    "process:referral": "PROCESS_REFERRAL",
    "process:internal-contact": "PROCESS_INTERNAL_CONTACT",
    "process:no-response-30d": "PROCESS_NO_RESPONSE_30D",
    "process:recruiter:outreach": "PROCESS_RECRUITER_OUTREACH",
}


def transform_application(doc: dict[str, Any]) -> dict[str, Any]:
    out = dict(doc)
    out["schemaVersion"] = 8

    if "status" in out and out["status"] in STATUS_MAP:
        out["status"] = STATUS_MAP[out["status"]]
    if "channel" in out and out["channel"] in CHANNEL_MAP:
        out["channel"] = CHANNEL_MAP[out["channel"]]

    # LocationDetail
    if "locationDetail" in out and isinstance(out["locationDetail"], dict):
        loc = dict(out["locationDetail"])
        if "workMode" in loc:
            raw_mode = loc.pop("workMode")
            loc["roleLocation"] = ROLE_LOCATION_MAP.get(raw_mode, raw_mode)
        if "roleLocation" in loc and loc["roleLocation"] in ROLE_LOCATION_MAP:
            loc["roleLocation"] = ROLE_LOCATION_MAP[loc["roleLocation"]]
        if "region" in loc and loc["region"] in REGION_MAP:
            loc["region"] = REGION_MAP[loc["region"]]
        out["locationDetail"] = loc

    # Tags
    if "tags" in out and isinstance(out["tags"], list):
        out["tags"] = [TAG_MAP.get(t, t) for t in out["tags"]]

    return out


def transform_event(doc: dict[str, Any]) -> dict[str, Any]:
    out = dict(doc)
    if "type" in out and out["type"] in EVENT_TYPE_MAP:
        out["type"] = EVENT_TYPE_MAP[out["type"]]
    if "statusFrom" in out and out["statusFrom"] in STATUS_MAP:
        out["statusFrom"] = STATUS_MAP[out["statusFrom"]]
    if "statusTo" in out and out["statusTo"] in STATUS_MAP:
        out["statusTo"] = STATUS_MAP[out["statusTo"]]
    if "actor" in out and out["actor"] in ACTOR_MAP:
        out["actor"] = ACTOR_MAP[out["actor"]]
    if "derivedFrom" in out and out["derivedFrom"] in DERIVED_FROM_MAP:
        out["derivedFrom"] = DERIVED_FROM_MAP[out["derivedFrom"]]
    if "confidence" in out and out["confidence"] in CONFIDENCE_MAP:
        out["confidence"] = CONFIDENCE_MAP[out["confidence"]]
    return out


def transform_artifact(doc: dict[str, Any]) -> dict[str, Any]:
    out = dict(doc)
    if "kind" in out and out["kind"] in ARTIFACT_KIND_MAP:
        out["kind"] = ARTIFACT_KIND_MAP[out["kind"]]
    return out


def transform_proposal(doc: dict[str, Any]) -> dict[str, Any]:
    out = dict(doc)
    if "kind" in out and out["kind"] in PROPOSAL_KIND_MAP:
        out["kind"] = PROPOSAL_KIND_MAP[out["kind"]]
    if "state" in out and out["state"] in PROPOSAL_STATE_MAP:
        out["state"] = PROPOSAL_STATE_MAP[out["state"]]
    if "trigger" in out and out["trigger"] in TRIGGER_MAP:
        out["trigger"] = TRIGGER_MAP[out["trigger"]]
    if "reviewedBy" in out and out["reviewedBy"] in ACTOR_MAP:
        out["reviewedBy"] = ACTOR_MAP[out["reviewedBy"]]
    if "changes" in out and isinstance(out["changes"], list):
        new_changes = []
        for ch in out["changes"]:
            c = dict(ch)
            if "statusFrom" in c and c["statusFrom"] in STATUS_MAP:
                c["statusFrom"] = STATUS_MAP[c["statusFrom"]]
            if "statusTo" in c and c["statusTo"] in STATUS_MAP:
                c["statusTo"] = STATUS_MAP[c["statusTo"]]
            if "eventType" in c and c["eventType"] in EVENT_TYPE_MAP:
                c["eventType"] = EVENT_TYPE_MAP[c["eventType"]]
            new_changes.append(c)
        out["changes"] = new_changes
    return out


def transform_sync_request(doc: dict[str, Any]) -> dict[str, Any]:
    out = dict(doc)
    if "kind" in out:
        out["kind"] = "EMAIL_COLLECT"
    if "state" in out and out["state"] in SYNC_STATE_MAP:
        out["state"] = SYNC_STATE_MAP[out["state"]]
    return out
