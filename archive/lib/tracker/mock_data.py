#!/usr/bin/env python3
"""Generate a full, fake tracker snapshot with the shape of a real one.

The goal is that a fresh setup looks like a long-used tracker: dozens of
applications across a year, every status and channel represented, salary and
fit data, artifacts, and a recent inbox read to feed the collector.

Two sources:

- `--from-csv PATH` reads a real legacy CSV (produce one with
  `tracker.py export --csv -o source.csv`) and replaces every identifying cell
  while keeping dates, statuses, channels, role types, fit ratings, and note
  lengths. The result has the same shape as the record it came from.
- without `--from-csv`, it synthesizes the same shape from a fixed template.

Every record is namespaced: company names start with `Mock `, so document IDs
start with `mock-`, and mock Gmail ids start with `mock`. Nothing else about
the data is special — the normal `import`, `collect`, `sheet --sync`, TUI, and
dashboard commands do all the work.

Output (under `--out`, default `generated/mock/`):

    job_search_tracker.csv
    gmail-inbox.json
    manifest.json

Remove exactly these records later with `--clear` (dry run) and
`--clear --apply`.

Everything is deterministic for a given `--seed` and `--reference-date`.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import json
import pathlib
import random
import sys

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO_ROOT / "scripts"))

import contract  # noqa: E402

DEFAULT_OUT = REPO_ROOT / "generated" / "mock"
DEFAULT_SEED = 20261031
DEFAULT_COUNT = 84
MOCK_COMPANY_PREFIX = "Mock "
MOCK_MESSAGE_PREFIX = "mock"

HEADER = [
    "id",
    "date",
    "company",
    "sector",
    "role",
    "role_type",
    "channel",
    "status",
    "contact_person",
    "fit_rating",
    "notes",
    "cv_file",
    "cover_letter_file",
    "source",
]

SECTORS = [
    "FinTech",
    "HealthTech",
    "Retail",
    "Logistics",
    "Media",
    "Education",
    "Energy",
    "Manufacturing",
    "Nonprofit",
    "Professional Services",
    "Gaming",
    "Travel",
    "Insurance",
    "Aerospace",
    "Biotech",
    "Public Sector",
]

ROLES = [
    ("Senior Android Engineer", "IC"),
    ("Staff Software Engineer", "IC"),
    ("Principal Mobile Engineer", "Lead"),
    ("Android Tech Lead", "Lead"),
    ("Senior Kotlin Developer", "IC"),
    ("Mobile Platform Architect", "Lead"),
    ("Engineering Manager", "Manager"),
    ("Senior React Native Engineer", "IC"),
    ("Android Security Engineer", "IC"),
    ("Director of Mobile", "Director"),
    ("Software Engineer II, Android", "IC"),
    ("Lead Mobile Developer", "Lead"),
]

CHANNELS = [
    "greenhouse",
    "workday",
    "lever",
    "ashby",
    "linkedin",
    "referral",
    "direct",
    "smartrecruiters",
]

STATUSES = [
    "rejected",
    "applied",
    "no_response",
    "interview",
    "withdrawn",
    "rejected",
    "applied",
    "offer",
    "rejected",
    "applied",
    "hired",
    "no_response",
    "interview",
    "rejected",
    "applied",
    "offer_declined",
    "drafting",
    "rejected",
]

NOTE_FRAGMENTS = [
    "Recruiter reached out after a conference talk.",
    "Referral from a former colleague on the platform team.",
    "Role is a close match on Kotlin and Compose; the domain is new.",
    "Process includes a take-home exercise and a system design loop.",
    "Compensation band was discussed on the first call.",
    "Team is mid-migration from XML views to Compose.",
    "Hybrid schedule, three days in office.",
    "Interview panel focused on offline-first architecture.",
    "Posting emphasizes developer experience and build tooling.",
    "Follow-up sent after two weeks without a response.",
    "Hiring manager wants someone to own the module split.",
    "Second round cancelled when the req was frozen.",
]

FIRST_NAMES = [
    "Avery",
    "Blake",
    "Cameron",
    "Dana",
    "Ellis",
    "Frankie",
    "Gray",
    "Harper",
    "Indigo",
    "Jules",
    "Kai",
    "Lane",
    "Marlow",
    "Noor",
    "Oakley",
    "Parker",
    "Quinn",
    "Reese",
    "Sasha",
    "Tatum",
]

LAST_NAMES = [
    "Ash",
    "Birch",
    "Cove",
    "Dale",
    "Elm",
    "Frost",
    "Grove",
    "Hale",
    "Ives",
    "Juno",
    "Kerr",
    "Lark",
    "Moss",
    "North",
    "Orr",
    "Pike",
    "Quill",
    "Rowe",
    "Stone",
    "Vale",
]

COMPANY_FIRST = [
    "Northwind",
    "Blue Harbor",
    "Cobalt",
    "Juniper",
    "Meridian",
    "Orchard",
    "Lattice",
    "Beacon",
    "Redwood",
    "Silverline",
    "Harborview",
    "Trailhead",
    "Copperfield",
    "Plainview",
    "Foxglove",
    "Ironwood",
    "Solstice",
    "Cardinal",
    "Marigold",
    "Kestrel",
    "Tidewater",
    "Brightpath",
    "Evergreen",
    "Lantern",
    "Quarry",
    "Vesper",
    "Alder",
    "Bramble",
    "Cedarworks",
    "Driftwood",
    "Emberlight",
    "Fernway",
    "Gable",
    "Hollow Creek",
    "Indigo",
    "Junco",
    "Kindling",
    "Larkspur",
    "Mosswell",
    "Nettle",
    "Opaline",
    "Pebblecove",
    "Quill & Compass",
    "Riverbend",
    "Sablecrest",
    "Thistlewood",
    "Umberline",
    "Violet Peak",
    "Willowbark",
    "Xenon",
    "Yarrow",
    "Zephyr",
]

COMPANY_SECOND = [
    "Analytics",
    "Bank",
    "Systems",
    "Health",
    "Robotics",
    "Pay",
    "Freight",
    "Insurance",
    "Retail",
    "Media",
    "Clinic",
    "Travel",
    "Energy",
    "Logistics",
    "Foods",
    "Manufacturing",
    "Labs",
    "Education",
    "Cosmetics",
    "Aerospace",
    "Utilities",
    "Legal",
    "Foundation",
    "Learning",
    "Software",
    "Networks",
    "Studio",
    "Hotels",
    "Games",
    "Transit",
    "Construction",
    "Brewing",
]

FILLER = [
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    "Ut enim ad minim veniam, quis nostrud exercitation ullamco.",
    "Duis aute irure dolor in reprehenderit in voluptate velit esse.",
    "Excepteur sint occaecat cupidatat non proident, sunt in culpa.",
    "Integer posuere erat a ante venenatis dapibus posuere velit aliquet.",
    "Cras mattis consectetur purus sit amet fermentum.",
    "Donec ullamcorper nulla non metus auctor fringilla.",
]

BOILERPLATE = "Added via historical gmail sync"


def isoDate(value: dt.date) -> str:
    return value.isoformat()


def noteFor(rng: random.Random, length: int, boilerplate: bool) -> str:
    if boilerplate:
        return BOILERPLATE
    parts = []
    while sum(len(part) + 1 for part in parts) < max(24, min(length, 300)):
        parts.append(rng.choice(NOTE_FRAGMENTS if rng.random() < 0.45 else FILLER))
        if len(parts) >= 6:
            break
    return " ".join(parts)


def companyPool(rng: random.Random, count: int) -> list[str]:
    """Unique fake company names, all namespaced with `Mock `.

    The first two form a deliberate ambiguity pair: "Mock Summit Robotics"
    also contains the standalone company "Mock Summit".
    """
    names = [f"{MOCK_COMPANY_PREFIX}Summit", f"{MOCK_COMPANY_PREFIX}Summit Robotics"]
    seen = {contract.slug(name) for name in names}
    combinations = [(first, second) for first in COMPANY_FIRST for second in COMPANY_SECOND]
    rng.shuffle(combinations)
    for first, second in combinations:
        if len(names) >= count:
            break
        candidate = f"{MOCK_COMPANY_PREFIX}{first} {second}"
        slug = contract.slug(candidate)
        if slug in seen:
            continue
        seen.add(slug)
        names.append(candidate)
    suffix = 2
    while len(names) < count:
        candidate = f"{MOCK_COMPANY_PREFIX}Summit Robotics Group {suffix}"
        suffix += 1
        slug = contract.slug(candidate)
        if slug in seen:
            continue
        seen.add(slug)
        names.append(candidate)
    return names


def readSourceCsv(path: pathlib.Path) -> list[dict]:
    with path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames != HEADER:
            raise SystemExit(
                f"{path} does not have the legacy tracker header.\n"
                f"  expected: {HEADER}\n"
                f"  actual:   {reader.fieldnames}\n"
                "Produce one with: .venv/bin/python scripts/tracker.py export --csv -o source.csv"
            )
        return list(reader)


def syntheticRows(count: int, reference: dt.date, rng: random.Random) -> list[dict]:
    rows = []
    for index in range(count):
        rows.append(
            {
                "date": isoDate(reference - dt.timedelta(days=index * 4 + 1)),
                "sector": rng.choice(SECTORS),
                "role": "",
                "role_type": "",
                "channel": rng.choice(CHANNELS),
                "status": STATUSES[index % len(STATUSES)],
                "fit_rating": str(55 + (index * 7) % 45),
                "notes": "",
                "has_cv": index % 4 != 3,
                "has_cover": index % 3 != 2,
            }
        )
    return rows


def anonymizeRows(source: list[dict], reference: dt.date, rng: random.Random) -> list[dict]:
    rows = []
    for row in source:
        rawDate = (row.get("date") or "").strip()
        try:
            date = dt.date.fromisoformat(rawDate) if rawDate else reference
        except ValueError:
            date = reference
        notes = (row.get("notes") or "").strip()
        rows.append(
            {
                "date": isoDate(date),
                "sector": rng.choice(SECTORS),
                "role": "",
                "role_type": (row.get("role_type") or "").strip(),
                "channel": (row.get("channel") or "").strip(),
                "status": (row.get("status") or "").strip() or "applied",
                "fit_rating": (row.get("fit_rating") or "").strip(),
                "notes": "",
                "notesLength": len(notes),
                "boilerplate": notes == BOILERPLATE,
                "has_cv": bool((row.get("cv_file") or "").strip()),
                "has_cover": bool((row.get("cover_letter_file") or "").strip()),
            }
        )
    return rows


def buildApplications(
    templates: list[dict],
    *,
    count: int,
    reference: dt.date,
    rng: random.Random,
) -> list[dict]:
    companies = companyPool(rng, max(count, len(templates)))
    applications = []
    usedIds: set[str] = set()
    for index in range(count):
        template = templates[index % len(templates)] if templates else {}
        company = companies[index]
        defaultRole, defaultType = ROLES[index % len(ROLES)]
        role = template.get("role") or defaultRole
        roleType = template.get("role_type") or defaultType
        documentId = contract.documentId(company, role)
        if documentId in usedIds:
            role = f"{role} {index + 1}"
            documentId = contract.documentId(company, role)
        usedIds.add(documentId)
        status = template.get("status") or STATUSES[index % len(STATUSES)]
        channel = template.get("channel") or CHANNELS[index % len(CHANNELS)]
        if channel.startswith("channel:"):
            channel = channel.split(":", 1)[1]
        notes = noteFor(
            rng,
            template.get("notesLength", 0),
            bool(template.get("boilerplate")),
        )
        applications.append(
            {
                "id": documentId,
                "date": template.get("date")
                or isoDate(reference - dt.timedelta(days=index * 4 + 1)),
                "company": company,
                "sector": template.get("sector") or rng.choice(SECTORS),
                "role": role,
                "role_type": roleType,
                "channel": channel,
                "status": status,
                "contact_person": f"{rng.choice(FIRST_NAMES)} {rng.choice(LAST_NAMES)}",
                "fit_rating": template.get("fit_rating", str(55 + (index * 9) % 45)),
                "notes": notes,
                "cv_file": f"https://example.com/mock/{documentId}/cv.pdf"
                if template.get("has_cv", True)
                else "",
                "cover_letter_file": f"https://example.com/mock/{documentId}/cover-letter.pdf"
                if template.get("has_cover", index % 3 != 2)
                else "",
                "source": "mock",
            }
        )
    return applications


def recentApplications(
    applications: list[dict], reference: dt.date, limit: int = 24
) -> list[dict]:
    window = reference - dt.timedelta(days=29)

    def parsed(application: dict) -> dt.date:
        try:
            return dt.date.fromisoformat(application["date"])
        except ValueError:
            return reference

    candidates = [application for application in applications if parsed(application) >= window]
    if not candidates:
        candidates = sorted(applications, key=parsed, reverse=True)[:limit]
    return sorted(candidates, key=parsed, reverse=True)[:limit]


def messageId(index: int) -> str:
    return f"{MOCK_MESSAGE_PREFIX}{index:012d}"


def inboxMessages(
    applications: list[dict], reference: dt.date, rng: random.Random
) -> list[dict]:
    """Matching mail for current applications, plus deliberate noise.

    This is the same shape `tracker.py collect` reads from any fetched inbox:
    only the fields the collector uses.
    """
    recent = recentApplications(applications, reference)
    messages: list[dict] = []

    def add(
        subject: str,
        sender: str,
        body: str,
        *,
        offsetDays: int = 0,
    ) -> None:
        index = len(messages)
        date = reference - dt.timedelta(days=max(0, offsetDays))
        messages.append(
            {
                "messageId": messageId(index),
                "subject": subject,
                "sender": sender,
                "snippet": f"{subject}\n\n{body}"[:800],
                "receivedAt": f"{date.isoformat()}T14:{index % 60:02d}:00+00:00",
            }
        )

    for position, application in enumerate(recent):
        company = application["company"]
        role = application["role"]
        slug = contract.slug(company)
        ats = ["greenhouse", "lever", "workday", "ashby"][position % 4]
        sender = f"no-reply@{ats}.example"
        offset = position % 20
        status = contract.normalizeStatus(application["status"])
        if status == "status:rejected":
            add(
                f"Update on your application at {company}",
                sender,
                f"Unfortunately, we are not moving forward with your application for {role}. "
                "We received many strong applications and have decided to proceed with other candidates.",
                offsetDays=offset,
            )
        elif status == "status:interview":
            add(
                f"Interview availability at {company}",
                sender,
                f"We would like to schedule a call to discuss the {role} role. "
                "Please share a few times that work for you this week.",
                offsetDays=offset,
            )
        elif status == "status:offer":
            add(
                f"Your offer from {company}",
                f"talent@{slug}.example",
                f"We are pleased to offer you the {role} position. The offer letter is attached for review.",
                offsetDays=offset,
            )
        elif status in ("status:applied", "status:no-response"):
            add(
                f"Thank you for applying to {company}",
                sender,
                f"We have received your application for {role} and will be in touch if your experience matches.",
                offsetDays=offset,
            )
        if position == 3:
            add(
                f"Next step: coding challenge for {company}",
                sender,
                "The next step is a take-home coding challenge. You will have one week to complete it.",
                offsetDays=offset + 2,
            )
        if position == 1:
            # "Mock Summit Robotics" also contains the standalone "Mock Summit",
            # so this message is deliberately ambiguous.
            add(
                "Status update from Mock Summit Robotics",
                "careers@mock-summit-robotics.example",
                "We have received your application and will follow up shortly.",
                offsetDays=offset + 1,
            )

    noise = [
        (
            "Your weekly job alert",
            "alerts@example.com",
            "Ten new roles match your saved search. View them in the app.",
        ),
        (
            "Receipt for your order",
            "orders@example.com",
            "Thanks for your purchase. Your receipt is attached.",
        ),
        (
            "Security alert for your account",
            "security@example.com",
            "A new sign-in was detected. If this was not you, reset your password.",
        ),
        (
            "You have 4 saved jobs",
            "noreply@example.com",
            "Your saved jobs are waiting. Apply before they close.",
        ),
        (
            "Webinar: building offline-first apps",
            "events@example.com",
            "Join us Thursday for a technical deep dive. Registration is free.",
        ),
        (
            "Delivery update",
            "shipping@example.com",
            "Your package is out for delivery and should arrive today.",
        ),
        (
            "A newsletter about architecture",
            "editor@example.com",
            "This week: module boundaries, build caching, and API design.",
        ),
        (
            "Your subscription renews soon",
            "billing@example.com",
            "Your plan renews next month at the current rate.",
        ),
    ]
    for subject, sender, text in noise:
        add(
            subject,
            sender,
            f"{text}\n\n" + " ".join(rng.sample(FILLER, 2)),
            offsetDays=rng.randrange(0, 25),
        )

    messages.sort(key=lambda message: message["receivedAt"], reverse=True)
    for index, message in enumerate(messages):
        message["messageId"] = messageId(index)
    return messages


def writeCsv(path: pathlib.Path, applications: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=HEADER)
        writer.writeheader()
        for application in applications:
            writer.writerow({key: application[key] for key in HEADER})


def writeJson(path: pathlib.Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def generate(
    *,
    out: pathlib.Path,
    seed: int,
    count: int,
    reference: dt.date,
    sourceCsv: pathlib.Path | None,
) -> dict:
    rng = random.Random(seed)
    if sourceCsv is not None:
        sourceRows = readSourceCsv(sourceCsv)
        count = len(sourceRows)
        templates = anonymizeRows(sourceRows, reference, rng)
        mode = "anonymized"
    else:
        templates = syntheticRows(count, reference, rng)
        mode = "synthetic"

    applications = buildApplications(templates, count=count, reference=reference, rng=rng)
    messages = inboxMessages(applications, reference, rng)

    out.mkdir(parents=True, exist_ok=True)
    writeCsv(out / "job_search_tracker.csv", applications)
    writeJson(
        out / "gmail-inbox.json",
        {
            "lookbackFrom": f"{reference.isoformat()}T00:00:00+00:00",
            "messages": messages,
        },
    )

    manifest = {
        "mode": mode,
        "seed": seed,
        "referenceDate": reference.isoformat(),
        "applications": len(applications),
        "messages": len(messages),
        "namespace": MOCK_COMPANY_PREFIX.strip(),
        "csv": str(out / "job_search_tracker.csv"),
        "inbox": str(out / "gmail-inbox.json"),
    }
    writeJson(out / "manifest.json", manifest)
    return manifest


def isMockCompany(company: str | None) -> bool:
    return bool(company) and str(company).startswith(MOCK_COMPANY_PREFIX)


def clear(*, apply: bool) -> dict:
    """Delete only namespaced mock records. Dry run unless `apply` is true.

    Cleans up:
    - Mock applications and their events/artifacts
    - Mock proposals in any state (pending, approved, rejected, expired)
      that reference *only* mock applications (mixed proposals are left alone)
    - Mock message IDs from meta/sync processed list
    """
    import client

    db = client.getDb()
    applications = list(db.collection(client.APPLICATIONS).stream())
    mockApplications = [
        snapshot for snapshot in applications if isMockCompany((snapshot.to_dict() or {}).get("company"))
    ]
    mockIds = {snapshot.id for snapshot in mockApplications}

    # Find proposals that reference *only* mock applications.
    # Mixed proposals (containing both mock and real IDs) are left alone.
    allProposals = list(db.collection(client.PENDING_PROPOSALS).stream())
    mockOnlyProposals = []
    for snapshot in allProposals:
        data = snapshot.to_dict() or {}
        scope = data.get("scope") or {}
        appIds = set(scope.get("applicationIds") or [])
        if appIds and appIds.issubset(mockIds):
            mockOnlyProposals.append(snapshot)

    syncSnapshot = db.collection(client.META).document(client.META_SYNC).get()
    processed = (
        ((syncSnapshot.to_dict() or {}).get("gmail") or {}).get("processedMessageIds") or []
        if syncSnapshot.exists
        else []
    )
    mockMessages = [message for message in processed if str(message).startswith(MOCK_MESSAGE_PREFIX)]

    counts = {
        "applications": len(mockIds),
        "mockOnlyProposals": len(mockOnlyProposals),
        "processedMessages": len(mockMessages),
    }

    if apply:
        client.assertWritesAllowed("mock data")
        for snapshot in mockApplications:
            for name in (client.EVENTS, client.ARTIFACTS):
                collection = snapshot.reference.collection(name)
                while True:
                    batch = list(collection.limit(400).stream())
                    if not batch:
                        break
                    operation = db.batch()
                    for document in batch:
                        operation.delete(document.reference)
                    operation.commit()
            snapshot.reference.delete()
        for snapshot in mockOnlyProposals:
            snapshot.reference.delete()
        if mockMessages:
            from google.cloud import firestore

            db.collection(client.META).document(client.META_SYNC).update(
                {"gmail.processedMessageIds": firestore.ArrayRemove(mockMessages)}
            )

    return {"applied": bool(apply), **counts}


def parseReference(value: str | None) -> dt.date:
    if not value:
        return dt.date.today()
    try:
        return dt.date.fromisoformat(value)
    except ValueError as exc:
        raise SystemExit(f"--reference-date must be YYYY-MM-DD, got {value!r}") from exc


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", default=str(DEFAULT_OUT), help="output directory")
    parser.add_argument("--seed", type=int, default=DEFAULT_SEED, help="deterministic seed")
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT, help="rows when synthesizing")
    parser.add_argument("--from-csv", help="anonymize this real legacy CSV instead of synthesizing")
    parser.add_argument(
        "--reference-date",
        help="newest date the mock record should know about (default: today, YYYY-MM-DD)",
    )
    parser.add_argument("--clear", action="store_true", help="delete namespaced mock records")
    parser.add_argument("--apply", action="store_true", help="with --clear, actually delete")
    args = parser.parse_args(argv)

    if args.clear:
        print(json.dumps({"ok": True, **clear(apply=args.apply)}))
        return 0

    manifest = generate(
        out=pathlib.Path(args.out).expanduser(),
        seed=args.seed,
        count=max(1, args.count),
        reference=parseReference(args.reference_date),
        sourceCsv=pathlib.Path(args.from_csv).expanduser() if args.from_csv else None,
    )
    print(json.dumps({"ok": True, **manifest}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
