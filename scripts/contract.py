"""The contract: one loader, one validator, one set of derived-field rules.

`tracker.schema.json` is the single source of truth for field shapes and enum
values. This module is the only place that reads it, so an enum can never be
half-updated: change the JSON and every validator, renderer, and Sheet header
moves with it.
"""

from __future__ import annotations

import json
import pathlib
import re
import unicodedata
import uuid

import jsonschema

from urls import clean_url

SCHEMA_PATH = pathlib.Path(__file__).resolve().parent.parent / "tracker.schema.json"

# Crockford base32 is gone; identifiers are UUIDv7. This is the canonical
# textual form, and it pins the two bits that matter: the version nibble is 7
# and the variant nibble is RFC 4122. A v4 UUID is a hard error rather than a
# silently unsortable record.
UUID_V7 = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
)
_SLUG_STRIP = re.compile(r"[^a-z0-9]+")

# --------------------------------------------------------------------------
# Directory name normalization
#
# Used for filesystem and Google Drive paths so that {company}/{role}/...
# directories are consistent regardless of how the human typed the name.
# Rules:
#   1. Expand common title contractions (Sr. -> Senior, etc.)
#   2. Replace commas with hyphens
#   3. Replace spaces and hyphens with underscores
#   4. Title-case each word
#   5. Strip leading/trailing underscores and collapse duplicates
#   6. Deduplicate adjacent identical words (e.g. "Android_Android" -> "Android")
# --------------------------------------------------------------------------

_DIR_CONTRACTIONS = {
    "sr": "Senior",
    "sr.": "Senior",
    "jr": "Junior",
    "jr.": "Junior",
    "asst": "Assistant",
    "asst.": "Assistant",
    "assoc": "Associate",
    "assoc.": "Associate",
    "eng": "Engineer",
    "eng.": "Engineer",
    "dev": "Developer",
    "dev.": "Developer",
    "mgt": "Management",
    "mgmt": "Management",
    "tech": "Technology",
    "tech.": "Technology",
    "ops": "Operations",
    "qa": "Quality Assurance",
    "ui": "User Interface",
    "ux": "User Experience",
    "devops": "DevOps",
    "devrel": "Developer Relations",
    "devex": "Developer Experience",
    "devx": "Developer Experience",
    "ml": "Machine Learning",
    "ai": "Artificial Intelligence",
    "vr": "Virtual Reality",
    "ar": "Augmented Reality",
    "iot": "Internet of Things",
    "saas": "Software as a Service",
    "api": "Application Programming Interface",
    "ios": "iOS",
}

_DIR_CONTRACTIONS = {
    "sr": "senior",
    "sr.": "senior",
    "jr": "junior",
    "jr.": "junior",
    "asst": "assistant",
    "asst.": "assistant",
    "assoc": "associate",
    "assoc.": "associate",
}

_STOP_WORDS = {"the", "a", "an", "of", "and"}
_CORP_SUFFIXES = {"inc", "llc", "corp", "corporation", "ltd", "co", "company"}
_DIR_BAD_WORDS = {"the", "a", "an", "of", "and", "or", "in", "on", "at", "to", "for"}

# Roman numerals
_ROMAN_NUMERALS = {"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x"}


def normalize_dir_name(raw: str) -> str:
    """Normalize a company or role name for use in lower-kebab-case directory paths.

    Rules:
      1. Strip parentheticals following a word like ' (MLB)'.
      2. Expand title contractions ('sr.' -> 'senior', 'jr.' -> 'junior', etc.).
      3. Strip stop words ('the', 'a', 'an', 'of', 'and') and corporate suffixes ('inc', 'llc', etc.).
      4. Convert to lower-kebab-case.

    Example transformations:
        "The Washington Post"             ->  "washington-post"
        "Scout Motors Inc."               ->  "scout-motors"
        "Sr. Software Engineer, Android"  ->  "senior-software-engineer-android"
        "Major League Baseball (MLB)"     ->  "major-league-baseball"
        "Snap Inc."                       ->  "snap"
        "EBay"                            ->  "ebay"
        "onX"                             ->  "onx"
    """
    if not raw:
        return ""

    text = re.sub(r"(?<=\w)\s*\([A-Za-z0-9]+\)", "", raw.strip())
    normalized = unicodedata.normalize("NFKD", text)
    asciiOnly = normalized.encode("ascii", "ignore").decode("ascii")
    raw_slug = _SLUG_STRIP.sub("-", asciiOnly.lower().strip()).strip("-")
    if not raw_slug:
        return ""
    tokens = raw_slug.split("-")

    expanded = []
    for token in tokens:
        if not token:
            continue
        if token in _DIR_CONTRACTIONS:
            expanded.append(_DIR_CONTRACTIONS[token])
        else:
            expanded.append(token)

    filtered = [w for w in expanded if w not in _STOP_WORDS and w not in _CORP_SUFFIXES]
    final_tokens = filtered or expanded

    deduped = []
    for word in final_tokens:
        if deduped and deduped[-1] == word:
            continue
        deduped.append(word)

    return "-".join(deduped)

_CACHE: dict | None = None
_VALIDATORS: dict[str, jsonschema.Draft202012Validator] = {}


class ContractError(ValueError):
    """A document does not satisfy tracker.schema.json."""


def load() -> dict:
    """Load and memoize the schema document."""
    global _CACHE
    if _CACHE is None:
        _CACHE = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    return _CACHE


def reload() -> dict:
    """Drop the memoized schema. Used by tests that mutate the file."""
    global _CACHE
    _CACHE = None
    _VALIDATORS.clear()
    return load()


def schemaVersion() -> int:
    return load()["schemaVersion"]


def definitions() -> dict:
    return load()["$defs"]


def definition(name: str) -> dict:
    try:
        return definitions()[name]
    except KeyError:
        raise ContractError(f"unknown schema definition {name!r}") from None


# --------------------------------------------------------------------------
# Enums
# --------------------------------------------------------------------------


def enums(name: str) -> list[str]:
    """Return the values of a named enum, e.g. enums("eventType")."""
    return list(definition(f"{name}Value")["enum"])


def statusValues() -> list[str]:
    return enums("status")


def statusRank() -> dict[str, int]:
    return dict(definition("statusValue")["x-status-rank"])


def terminalStatuses() -> set[str]:
    return set(definition("statusValue")["x-terminal"])


def tags() -> list[str]:
    return enums("tag")


def regions() -> list[str]:
    return enums("region")


def legacyStatusMap() -> dict[str, str]:
    return dict(load()["x-legacy-csv"]["statusMap"])


def legacyFieldMap() -> dict[str, str]:
    return dict(load()["x-legacy-csv"]["fieldMap"])


def normalizeStatus(raw: str) -> str:
    """Map a legacy or human status onto a canonical status value.

    Accepts 'applied', 'APPLIED', 'no response', and legacy namespaced values,
    so callers do not have to know which era a value came from.
    """
    if not raw:
        raise ContractError("status is empty")
    value = raw.strip()
    values = statusValues()
    if value in values:
        return value
    cleaned = value.lower().removeprefix("status:").strip()
    if cleaned.upper() in values:
        return cleaned.upper()
    key = cleaned.replace("_", " ").strip()
    mapping = legacyStatusMap()
    if cleaned in mapping:
        return mapping[cleaned]
    if key in mapping:
        return mapping[key]
    underscored = key.replace(" ", "_")
    if underscored in mapping:
        return mapping[underscored]
    # Check special mappings
    if cleaned in ("offer", "offer_expected", "offer-expected"):
        return "OFFER_EXPECTED"
    if cleaned in ("withdrawn", "application_withdrawn", "application-withdrawn"):
        return "APPLICATION_WITHDRAWN"
    if cleaned in ("no-response", "no_response", "no response"):
        return "NO_RESPONSE"
    if cleaned in ("offer-declined", "offer_declined", "offer declined"):
        return "OFFER_DECLINED"
    raise ContractError(
        f"status {raw!r} is not recognized. Known values: {sorted(values)}"
    )


def isTerminal(status: str) -> bool:
    return normalizeStatus(status) in terminalStatuses()


# --------------------------------------------------------------------------
# Identifiers
# --------------------------------------------------------------------------


def slug(value: str, fallback: str = "") -> str:
    """ASCII kebab slug, used for companyId and document IDs.
    
    Normalizes to lower-kebab-case, stripping stop words ('the', 'a', 'an', 'of', 'and'),
    corporate suffixes ('inc', 'llc', etc.), and parentheticals like '(MLB)'.
    """
    if not value:
        return fallback
    return normalize_dir_name(value) or fallback


def documentId(company: str, role: str | None) -> str:
    """Stable application ID: slug(company) + '--' + slug(role), or bare company.

    Byte-identical to the scheme already present in the CSV, which is what lets
    the Phase 3 reconciliation assert an exact match on all 84 rows.
    """
    companySlug = slug(company)
    if not companySlug:
        raise ContractError("cannot derive a document ID without a company name")
    roleSlug = slug(role or "")
    return f"{companySlug}--{roleSlug}" if roleSlug else companySlug


def newUuid() -> str:
    """Generate a local UUIDv7.

    Generated here rather than delegated to a service, because an identifier
    must never require a network round trip: the record has to be writable
    offline, and a failed ID allocation must not become a failed application.

    UUIDv7 carries a 48-bit millisecond timestamp in its high bits, so the
    textual form sorts in creation order. CPython's `uuid.uuid7()` is monotonic
    within a millisecond (it increments a counter rather than redrawing random
    bits), which is what lets `events` be ordered by document ID alone.
    """
    return str(uuid.uuid7())


def isUuid(value: str) -> bool:
    """True for the canonical lowercase UUIDv7 form."""
    return bool(UUID_V7.match(value or ""))


def uuidTimestamp(value: str) -> int:
    """Recover the millisecond timestamp encoded in a UUIDv7's high 48 bits."""
    if not isUuid(value):
        raise ContractError(f"not a UUIDv7: {value!r}")
    return uuid.UUID(value).int >> 80


# --------------------------------------------------------------------------
# Derived fields
# --------------------------------------------------------------------------


def deriveStatusFields(status: str) -> dict:
    """Compute statusRank and isOpen from a status value. Never hand-maintain these."""
    normalized = normalizeStatus(status)
    return {
        "status": normalized,
        "statusRank": statusRank()[normalized],
        "isOpen": normalized not in terminalStatuses(),
    }


# --------------------------------------------------------------------------
# Validation
# --------------------------------------------------------------------------


def validator(name: str) -> jsonschema.Draft202012Validator:
    """Return a memoized validator for a named definition."""
    if name not in _VALIDATORS:
        schema = load()
        _VALIDATORS[name] = jsonschema.Draft202012Validator(
            {"$ref": f"#/$defs/{name}", "$defs": schema["$defs"]}
        )
    return _VALIDATORS[name]


def errors(name: str, document: dict) -> list[str]:
    """Return human-readable validation errors for a document. Empty means valid."""
    messages = []
    for error in validator(name).iter_errors(document):
        location = "/".join(str(part) for part in error.absolute_path) or "<root>"
        messages.append(f"{location}: {error.message}")
    return messages


def validate(name: str, document: dict) -> dict:
    """Validate or raise. Returns the document for convenient chaining."""
    problems = errors(name, document)
    if problems:
        joined = "\n  - ".join(problems)
        raise ContractError(f"{name} does not satisfy the contract:\n  - {joined}")
    return document


# --------------------------------------------------------------------------
# Round-tripping Firestore values
# --------------------------------------------------------------------------


def toFirestore(document: dict):
    """Convert ISO date strings into Firestore Timestamps, recursively.

    The contract validates the JSON boundary (ISO strings) because that is what
    a user or an agent actually writes. Firestore needs Timestamps for range
    queries and ordering, so conversion happens once, here.
    """

    def convert(value):
        if isinstance(value, dict):
            return {key: convert(child) for key, child in value.items()}
        if isinstance(value, list):
            return [convert(child) for child in value]
        if isinstance(value, str) and re.fullmatch(
            r"\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2}(\.\d{1,9})?(Z|[+-]\d{2}:\d{2}))?",
            value,
        ):
            from datetime import datetime

            if "T" not in value:
                # Date-only. A naive UTC midnight renders as the same calendar
                # day everywhere, so a submission date never drifts backwards
                # for a reader west of UTC.
                parsed = datetime.fromisoformat(value)
            else:
                parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
            return parsed
        return value

    return convert(document)


def fromFirestore(value):
    """Convert Firestore Timestamps back into ISO strings for the JSON boundary."""
    from datetime import datetime

    if isinstance(value, dict):
        return {key: fromFirestore(child) for key, child in value.items()}
    if isinstance(value, list):
        return [fromFirestore(child) for child in value]
    if isinstance(value, datetime):
        return value.isoformat()
    return value
