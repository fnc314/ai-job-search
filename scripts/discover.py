#!/usr/bin/env python3
"""Discover untracked job applications from email evidence.

Scans the emails collection and cached inbox for messages that reference
companies not yet tracked, extracts role and status information, and stages
discovery proposals for review.

Usage:
    python3 scripts/discover.py [--all] [--since YYYY-MM-DD] [--dry-run]
"""

from __future__ import annotations

import _guard
_guard.enforce_mise()

import argparse
import json
import os
import re
import sys
import pathlib

SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import client  # noqa: E402
import contract  # noqa: E402
import proposals as proposalsModule  # noqa: E402


def emit(payload) -> None:
    """Print JSON to stdout."""
    json.dump(payload, sys.stdout, indent=2, ensure_ascii=False, sort_keys=False)
    sys.stdout.write("\n")


def discover(db, *, since: str | None = None, dryRun: bool = False) -> dict:
    """Scan emails collection for untracked applications and stage proposals."""
    from google.cloud.firestore_v1.base_query import FieldFilter

    # Get all tracked company IDs
    apps = {
        snap.id: contract.fromFirestore(snap.to_dict())
        for snap in db.collection(client.APPLICATIONS).stream()
    }
    tracked_company_ids = {
        app.get("companyId", "").lower()
        for app in apps.values()
        if app.get("companyId")
    }
    tracked_companies = {
        app.get("company", "").lower()
        for app in apps.values()
        if app.get("company")
    }

    # Query ALL emails with no applicationId — including NOISE, since noise
    # classification can be wrong (e.g., "order" matching "border" in HTML/CSS)
    emails_query = db.collection("emails").where(filter=FieldFilter("applicationId", "==", None))
    emails_docs = list(emails_query.stream())

    candidates = []
    company_groups = {}

    for doc in emails_docs:
        data = contract.fromFirestore(doc.to_dict())
        status = data.get("status", "UNCLASSIFIED")

        # Skip only if truly noise — re-classify by checking for job signals
        if status == "NOISE":
            if not _has_job_signals(data):
                continue

        received_at = data.get("receivedAt", "") or ""
        if since and received_at:
            date_part = received_at.split("T")[0].split("t")[0]
            if date_part < since:
                continue

        # Only process emails with strong job signals — skip everything else
        if not _has_job_signals(data):
            continue

        # Extract company from the email
        company = _extract_company(data)
        if not company:
            continue

        company_lower = company.lower()
        if company_lower in tracked_companies:
            continue

        # Check if companyId is already tracked
        company_id = contract.slug(company)
        if company_id.lower() in tracked_company_ids:
            continue

        # Fuzzy check: if the extracted company name contains or is contained by
        # a tracked company name, skip it (e.g., "Vanguardhr" contains "Vanguard")
        if _company_matches_tracked(company_lower, company_id, tracked_companies, tracked_company_ids):
            continue

        # Check if the email content references a tracked company — the extracted
        # company name may be wrong (e.g., ATS local part) while the snippet has
        # the real company name (e.g., "Capital One" in the email body)
        if _email_references_tracked(data, tracked_companies):
            continue

        if company_lower not in company_groups:
            company_groups[company_lower] = {
                "company": company,
                "companyId": company_id,
                "emails": []
            }
        company_groups[company_lower]["emails"].append(data)

    # Build discovery changes from grouped emails
    changes = []
    for company_lower, group in company_groups.items():
        emails = group["emails"]
        primary = emails[0]
        role = _extract_role(emails)
        status = _infer_status(emails)

        message_id = primary.get("id") or primary.get("messageId", "")
        subject = primary.get("subject", "")
        sender = primary.get("from", "")
        received_at = primary.get("receivedAt")

        changes.append({
            "applicationId": None,
            "eventType": "DISCOVER",
            "statusFrom": None,
            "statusTo": status,
            "confidence": "MEDIUM_CONFIDENCE",
            "discoveredCompany": group["company"],
            "discoveredCompanyId": group["companyId"],
            "discoveredRole": role,
            "sourceEmail": {
                "messageId": message_id,
                "subject": subject,
                "sender": sender,
                "receivedAt": received_at,
            },
            "relatedEmails": [
                e.get("id") or e.get("messageId", "")
                for e in emails[1:]
            ],
        })

    if not changes:
        return {
            "status": "SUCCESS",
            "candidates": 0,
            "message": "No untracked applications found",
        }

    # Build proposal
    import datetime as dt
    timestamp = dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds")
    proposal = {
        "proposalId": contract.newUuid(),
        "createdAt": timestamp,
        "dueAt": (dt.datetime.fromisoformat(timestamp) + dt.timedelta(days=1)).isoformat(
            timespec="seconds"
        ),
        "collectedAt": timestamp,
        "kind": "DISCOVER",
        "state": "PENDING",
        "trigger": "DISCOVERY",
        "scope": {
            "applicationIds": [],
            "lookbackFrom": None,
            "query": None,
        },
        "changes": changes,
        "unmatched": [],
    }

    if dryRun:
        return {"dryRun": True, "proposal": proposal}

    # Stage proposal
    db.collection(client.PENDING_PROPOSALS).document(proposal["proposalId"]).set(
        contract.toFirestore(proposal)
    )

    return {
        "status": "SUCCESS",
        "proposalId": proposal["proposalId"],
        "candidates": len(changes),
        "changes": len(changes),
    }


def _company_matches_tracked(
    company_lower: str,
    company_id: str,
    tracked_companies: set[str],
    tracked_company_ids: set[str],
) -> bool:
    """Check if an extracted company name fuzzy-matches a tracked company.
    
    Handles cases like:
    - "Vanguardhr" contains "Vanguard"
    
    Uses minimum length thresholds to avoid false matches on short names
    (e.g., "snap" matching "teamsnap" when the email is from Snapchat).
    """
    # Check if extracted name contains a tracked company name (min 5 chars)
    for tracked in tracked_companies:
        if len(tracked) >= 5 and tracked in company_lower:
            return True
        if len(company_lower) >= 5 and company_lower in tracked:
            return True
    # Check if extracted slug contains a tracked slug (min 5 chars)
    for tracked_id in tracked_company_ids:
        if len(tracked_id) >= 5 and tracked_id in company_id:
            return True
        if len(company_id) >= 5 and company_id in tracked_id:
            return True
    return False


def _email_references_tracked(
    email: dict,
    tracked_companies: set[str],
) -> bool:
    """Check if the email references a tracked company.
    
    The extracted company name may be wrong (e.g., ATS local part like "Myworkday")
    while the email subject or sender contains the real company name.
    
    Checks subject line and sender address to avoid false matches in HTML snippets.
    """
    subject = (email.get("subject") or "").lower()
    sender = (email.get("from") or "").lower()
    
    # Check subject for tracked company names (min 6 chars to avoid false matches)
    for tracked in tracked_companies:
        if len(tracked) > 6 and tracked in subject:
            return True
        # Also check without spaces (e.g., "capital one" -> "capitalone")
        tracked_nospace = tracked.replace(" ", "")
        if len(tracked_nospace) > 6 and tracked_nospace in subject:
            return True
    
    # Check sender for tracked company names (min 4 chars)
    for tracked in tracked_companies:
        if len(tracked) > 4 and tracked in sender:
            return True
        # Also check without spaces (e.g., "capital one" -> "capitalone")
        tracked_nospace = tracked.replace(" ", "")
        if len(tracked_nospace) > 4 and tracked_nospace in sender:
            return True
    
    return False


def _extract_company(email: dict) -> str | None:
    """Extract company name from email data.
    
    Tries multiple sources in order of confidence:
    1. Company field already set on the email document
    2. Subject/snippet patterns (company name in the email content)
    3. Sender domain (only for direct company domains, not ATS)
    4. Sender local part (only for ATS domains, last resort)
    """
    subject = (email.get("subject") or "")
    sender = (email.get("from") or "")
    snippet = (email.get("snippet") or "")
    combined = f"{subject} {sender} {snippet}"

    # 1. Check if company is already set on the email document
    if email.get("company"):
        return email["company"]

    # 2. Subject/snippet patterns — highest confidence for company name
    company_patterns = [
        r"update on your (?:job )?application at ([^\s,]+)",
        r"your application to ([^\s,]+)",
        r"application status at ([^\s,]+)",
        r"regarding your application at ([^\s,]+)",
        r"your application with ([^\s,]+)",
        r"from ([^\s,]+) recruiting",
        r"from ([^\s,]+) careers",
        r"from ([^\s,]+) talent",
        r"from ([^\s,]+) hiring",
        r"from ([^\s,]+) hr",
        r"application for ([^\s,]+) (?:position|role|opening)",
        r"at ([^\s,]+) (?:position|role|opening)",
    ]

    for pattern in company_patterns:
        match = re.search(pattern, combined, re.IGNORECASE)
        if match:
            company = match.group(1).strip()
            if len(company) > 2:
                return company

    # 3. Extract company from sender — prefer direct company domains over ATS
    sender_company = _extract_company_from_sender(sender)
    if sender_company:
        return sender_company

    return None


def _extract_company_from_sender(sender: str) -> str | None:
    """Extract company name from the sender email address.
    
    Handles patterns like:
    - fabletics@myworkday.com -> fabletics
    - taylor_vanepps@baxter.com -> baxter
    - notifications@career-mail.aarp.org -> aarp
    
    For ATS domains, only returns the local part if it looks like a
    single-word company name (not a person's name or generic HR username).
    """
    # Extract email from "Display Name <email@domain.com>" format
    import re as re_module
    email_match = re_module.search(r"<([^>]+)>", sender)
    if email_match:
        email_addr = email_match.group(1)
    else:
        email_addr = sender
    
    if "@" not in email_addr:
        return None
    
    local_part, domain = email_addr.rsplit("@", 1)
    
    # Known ATS domains — extract company from local part only if it
    # looks like a company name (single word, not a person's name)
    ats_domains = {
        "greenhouse.io", "lever.co", "myworkday.com", "ashbyhq.com",
        "smartrecruiters.com", "icims.com", "bamboohr.com",
        "workday.com", "talent.neptun.so",
        "greenhouse-mail.io", "nurture.icims.com",
    }
    
    if domain in ats_domains:
        # Clean up local part
        company = local_part.strip().split("+")[0]  # strip +tag
        # Only accept if it's a single word (likely a company name)
        # Reject names with underscores, dots, or multiple words (person names)
        if re.match(r"^[a-zA-Z][a-zA-Z0-9]*$", company):
            # Reject common non-company local parts
            reject_locals = {
                "capitalone", "capitalonehr", "capitalonehrworkday",
                "noreply", "no-reply", "notifications", "crewcentral",
                "crewcentral_noreply", "support", "admin", "hr",
                "careers", "recruiting", "hiring", "talent",
            }
            if company.lower() in reject_locals:
                return None
            return company.capitalize()
        # Otherwise, ATS local part is a person's name or generic username — skip
        return None
    
    # Direct company domains (not ATS)
    # Skip common non-company domains
    skip_domains = {
        "linkedin.com", "glassdoor.com", "indeed.com", "monster.com",
        "dice.com", "ziprecruiter.com", "careerbuilder.com",
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
        "googlemail.com", "apple.com", "google.com", "microsoft.com", "meta.com",
        "aol.com", "venmo.com", "docker.com", "obsidian.md",
        "adguard-dns.io", "codetriage.com", "hunter.io",
        "neighborhoodalerts.com", "hcs.com",
        "ahn.org", "morganstanley.com", "alliespgh.org",
        "centraprise.com", "marriotthiring.com",
        "wellsfargorewards.com", "fidelity.com",
    }
    
    if domain in skip_domains:
        return None
    
    # Extract company from domain (strip www, mail, career-mail, etc.)
    domain_parts = domain.split(".")
    if len(domain_parts) >= 2:
        # Handle subdomains like career-mail.aarp.org -> aarp
        # Take the second-level domain
        company = domain_parts[-2] if len(domain_parts) >= 2 else domain_parts[0]
        # Clean up common prefixes
        company = re.sub(r"^(mail|career|hr|jobs|recruit|crewcentral|noreply)-?", "", company)
        if company and len(company) > 2:
            return company.capitalize()
    
    return None


def _has_job_signals(email: dict) -> bool:
    """Check if an email has strong job-related signals.
    
    Used to re-classify NOISE emails that were false positives, and to
    filter out weak signals (talent network emails, job alerts, etc.).
    
    Returns True only for emails that clearly reference a specific job
    application, not just any career-related email.
    """
    subject = (email.get("subject") or "").lower()
    sender = (email.get("from") or "").lower()
    snippet = (email.get("snippet") or "").lower()
    combined = f"{subject} {snippet}"
    
    # Reject emails from personal email addresses FIRST — replies/forwards
    # from personal accounts are never discovery candidates
    personal_domains = {
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
        "googlemail.com", "aol.com", "icloud.com",
    }
    sender_email = sender
    if "<" in sender and ">" in sender:
        sender_email = sender[sender.index("<") + 1:sender.index(">")]
    sender_domain = sender_email.rsplit("@", 1)[-1].strip(">").lower() if "@" in sender_email else ""
    if sender_domain in personal_domains:
        return False
    
    # Strong job application signals — specific to an application
    strong_signals = [
        "update on your job application",
        "your application to",
        "application status",
        "thank you for applying",
        "application received",
        "not moving forward",
        "no longer under consideration",
        "not selected",
        "position has been filled",
        "position was filled",
        "job offer",
        "pleased to offer",
        "offer letter",
        "assessment",
        "coding challenge",
    ]
    
    # Interview signals — only if they reference a specific role
    interview_signals = [
        "interview request",
        "interview invitation",
        "phone screen",
        "schedule a call",
        "schedule an interview",
        "interview confirmation",
    ]
    
    if any(signal in combined for signal in strong_signals):
        return True
    
    if any(signal in combined for signal in interview_signals):
        return True
    
    # Reject known non-application patterns BEFORE checking ATS weak signals
    reject_patterns = [
        "talent network",
        "talent scout digest",
        "job alert",
        "new jobs for",
        "jobs matching",
        "recommended for you",
        "people also viewed",
        "viewed your profile",
        "someone viewed",
        "invitation to connect",
        "connection request",
        "newsletter",
        "digest",
        "security alert",
        "sign-in",
        "password reset",
        "receipt",
        "payment",
        "invoice",
        "new job opportunities",
        "based on your search",
        "here are some new roles",
    ]
    if any(pattern in combined for pattern in reject_patterns):
        return False
    
    # ATS sender domains — but only if subject also has job signals
    ats_domains = [
        "greenhouse.io", "lever.co", "myworkday.com", "ashbyhq.com",
        "smartrecruiters.com", "icims.com", "bamboohr.com",
        "workday.com", "talent.neptun.so",
    ]
    if any(domain in sender for domain in ats_domains):
        # ATS emails are likely job-related, but check for weak signals too
        weak_ats_signals = [
            "application",
            "position",
            "role",
            "job",
            "career",
            "hiring",
            "recruit",
        ]
        if any(signal in combined for signal in weak_ats_signals):
            return True

    return False


def _extract_role(emails: list[dict]) -> str:
    """Extract role title from email subjects and snippets."""
    import re
    role_patterns = [
        r"position[:\s]+([^,\n]+)",
        r"role[:\s]+([^,\n]+)",
        r"job title[:\s]+([^,\n]+)",
        r"for the (\S+) (?:position|role|opening)",
        r"application for ([^,\n]+)",
    ]

    for email in emails:
        subject = email.get("subject", "")
        snippet = email.get("snippet", "")
        combined = f"{subject} {snippet}"

        for pattern in role_patterns:
            match = re.search(pattern, combined, re.IGNORECASE)
            if match:
                role = match.group(1).strip()
                if 2 < len(role) < 100:
                    return role

    return "Unknown / To Be Confirmed"


def _infer_status(emails: list[dict]) -> str:
    """Infer application status from email content."""
    rejection_kw = [
        "unfortunately", "not moving forward", "regret",
        "no longer under consideration", "not selected",
        "won't be moving forward", "will not be moving forward",
        "position has been filled", "position was filled",
    ]
    offer_kw = ["job offer", "pleased to offer", "offer letter"]
    interview_kw = ["interview", "phone screen", "schedule a call"]
    ack_kw = ["thank you for applying", "application received"]

    for email in emails:
        combined = f"{email.get('subject', '')} {email.get('snippet', '')}".lower()
        if any(kw in combined for kw in rejection_kw):
            return "REJECTED"
        if any(kw in combined for kw in offer_kw):
            return "OFFER"
        if any(kw in combined for kw in interview_kw):
            return "INTERVIEWING"
        if any(kw in combined for kw in ack_kw):
            return "SUBMITTED"

    return "SUBMITTED"


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Discover untracked job applications from email evidence"
    )
    parser.add_argument("--all", action="store_true", help="Scan all cached emails")
    parser.add_argument("--since", type=str, help="Only scan emails since YYYY-MM-DD")
    parser.add_argument("--dry-run", action="store_true", help="Preview without writing")
    args = parser.parse_args()

    if not os.environ.get("TRACKER_ALLOW_WRITES") and not args.dry_run:
        print(
            "Error: set TRACKER_ALLOW_WRITES=1 to write proposals, or use --dry-run",
            file=sys.stderr,
        )
        return 2

    try:
        db = client.getDb()
        result = discover(
            db,
            since=args.since,
            dryRun=args.dry_run,
        )
        emit(result)
        return 0
    except Exception as e:
        emit({"status": "ERROR", "message": str(e)})
        return 1


if __name__ == "__main__":
    sys.exit(main())
