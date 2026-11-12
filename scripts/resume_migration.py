#!/usr/bin/env python3
"""Migrate untracked resume artifacts into the job-applications/ directory structure.

This script operates in three modes:

1. **--scan** (default): Scans /Users/fnc314/Documents/Resumes, extracts company/role/date
   metadata, cross-references Firestore, and outputs:
   - generated/resume-migration-manifest.json
   - UNRESOLVED.md (files needing manual review)

2. **--apply**: Reads the manifest, creates directories, copies files into job-applications/.

3. **--upload**: Reads the manifest, uploads all copied files to GCS using
   artifacts.ensureUpload() for idempotent, hash-deduplicated uploads.

Usage:
    python scripts/resume_migration.py --scan
    python scripts/resume_migration.py --apply
    TRACKER_ALLOW_WRITES=1 python scripts/resume_migration.py --upload
"""

from __future__ import annotations

import argparse
import json
import os
import pathlib
import re
import shutil
import sys
from datetime import datetime

SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import client
import contract

# Source directory containing untracked resume artifacts
RESUMES_DIR = pathlib.Path("/Users/fnc314/Documents/Resumes")

# Repository root
REPO_ROOT = SCRIPT_DIR.parent

# Generated manifest path
MANIFEST_PATH = REPO_ROOT / "generated" / "resume-migration-manifest.json"

# Unresolved files report
UNRESOLVED_PATH = REPO_ROOT / "UNRESOLVED.md"

# Job applications target directory
JOB_APPS_DIR = REPO_ROOT / "job-applications"

# Known company names for role-before-company detection
KNOWN_COMPANIES = {
    "meta", "google", "apple", "amazon", "microsoft", "netflix", "slack",
    "anthropic", "openai", "salesforce", "capitalone", "capital-one",
    "jpmorgan", "jpmorganchase", "jpmorgan-chase", "edwardjones", "edward-jones",
    "eightsleep", "eight-sleep", "homedepot", "home-depot", "thompsonreuters",
    "thomson-reuters", "unit21", "yinzcam", "colossus", "colossustechnologies",
    "colossus-technologies", "fitnesstech", "fitness-tech", "planetfitness",
    "planet-fitness", "paradigmnatl", "paradigm-natl", "spoton", "duolingo",
    "pinterest", "block", "lyft", "motorola", "redfin", "hightouch", "jagger",
    "temu", "toast", "waymo", "npr", "nextdoor", "reddit", "pnc", "ford",
    "future", "bitwarden", "1password", "classlist", "peleton", "anthropic",
}

# Subdir-to-company mapping for ambiguous directories
SUBDIR_COMPANY_MAP = {
    "salesforce-slack": "salesforce",
}


def split_camel_case(text: str) -> str:
    """Split camelCase/PascalCase into space-separated words.
    
    CapitalOne -> Capital One, JPMorganChase -> JP Morgan Chase
    """
    # Insert space before uppercase letters that follow lowercase
    result = re.sub(r"([a-z])([A-Z])", r"\1 \2", text)
    # Insert space before uppercase letters that precede lowercase (for acronyms)
    result = re.sub(r"([A-Z]+)([A-Z][a-z])", r"\1 \2", result)
    return result


def get_file_date(file_path: pathlib.Path) -> str:
    """Get file creation date as YYYY-MM-DD."""
    try:
        birth = file_path.stat().st_birthtime
        return datetime.fromtimestamp(birth).strftime("%Y-%m-%d")
    except (OSError, AttributeError):
        pass
    # Fallback to ctime
    return datetime.fromtimestamp(file_path.stat().st_ctime).strftime("%Y-%m-%d")


def parse_resume_edits_header(file_path: pathlib.Path) -> tuple[str | None, str | None]:
    """Parse RESUME EDITS header to extract company and role.
    
    Format: RESUME EDITS: {Company} - {Role}
    """
    try:
        content = file_path.read_text(encoding="utf-8", errors="replace")
        first_line = content.split("\n")[0].strip()
        match = re.match(r"RESUME EDITS:\s*(.+?)\s*-\s*(.+)", first_line, re.IGNORECASE)
        if match:
            company = match.group(1).strip()
            role = match.group(2).strip()
            return company, role
    except (OSError, IOError):
        pass
    return None, None


def extract_company_from_filename(name: str) -> tuple[str | None, str | None, str]:
    """Extract company and role from filename patterns.
    
    Returns (company_raw, role_raw, source_pattern).
    """
    # Remove extension for matching
    stem = pathlib.Path(name).stem
    
    # Pattern: FrancoNColaizzi_Resume_{Company}[_{Role}]
    # Allow company names starting with digits (e.g., 1Password)
    m = re.match(
        r"FrancoNColaizzi_Resume_([A-Za-z0-9][A-Za-z0-9]+(?:[A-Za-z][A-Za-z0-9]*)*)"
        r"(?:_([A-Za-z][A-Za-z0-9]+(?:[A-Za-z][A-Za-z0-9]*)*))?$",
        stem,
    )
    if m:
        company_raw = m.group(1)
        role_raw = m.group(2)
        
        # Check for role-before-company pattern
        # e.g., SoftwareEngineerAndroidTechnicalLeadership_Meta -> company=Meta, role=SoftwareEngineerAndroidTechnicalLeadership
        # Also check if the "role" is actually a company name (meaning company field has the role)
        tokens = re.split(r"([A-Z][a-z]+|[A-Z]+)", company_raw)
        tokens = [t for t in tokens if t]
        
        if len(tokens) >= 2:
            last_token = tokens[-1]
            last_slug = contract.slug(last_token)
            if last_slug in KNOWN_COMPANIES:
                # Last token is a known company — role-before-company pattern
                role_raw = "".join(tokens[:-1])
                company_raw = last_token
        
        # Also check if the extracted "role" is actually a company name
        if role_raw:
            role_slug = contract.slug(role_raw)
            if role_slug in KNOWN_COMPANIES:
                # Swap: what we thought was the role is actually the company
                company_raw, role_raw = role_raw, company_raw
        
        return company_raw, role_raw, "franco_resume"

    # Pattern: COLAIZZI_FRANCO_Res_{Company}{Role} (concatenated)
    m = re.match(
        r"COLAIZZI_FRANCO_Res_([A-Za-z][A-Za-z0-9]+(?:[A-Za-z][A-Za-z0-9]*)*)$",
        stem,
    )
    if m:
        combined = m.group(1)
        # Try to split into company + role
        # Known patterns: SpotOnStaffSoftwareEngineer -> SpotOn + StaffSoftwareEngineer
        # Strategy: try known company prefixes
        for known in sorted(KNOWN_COMPANIES, key=len, reverse=True):
            known_camel = known.replace("-", "")
            if combined.lower().startswith(known_camel.lower()):
                company_raw = combined[:len(known_camel)]
                role_raw = combined[len(known_camel):]
                if role_raw:
                    return company_raw, role_raw, "colaizzi_concatenated"
        # Fallback: entire thing is company
        return combined, None, "colaizzi"

    # Pattern: COLAIZZI_FRANCO_Res_V{N}-{Company}_{Role}
    m = re.match(
        r"COLAIZZI_FRANCO_Res_V\d+-([A-Za-z][A-Za-z0-9]+)_([A-Za-z][A-Za-z0-9,]+)$",
        stem,
    )
    if m:
        return m.group(1), m.group(2), "colaizzi_versioned"

    # Pattern: COLAIZZI_FRANCO_Res_{Company}
    m = re.match(
        r"COLAIZZI_FRANCO_Res_([A-Za-z][A-Za-z0-9]+(?:[A-Za-z][A-Za-z0-9]*)*)$",
        stem,
    )
    if m:
        return m.group(1), None, "colaizzi"

    # Pattern: {Company} - {Role} - ...
    m = re.match(r"^([A-Za-z][A-Za-z]+)\s*-\s*(.+?)\s*-\s*", name)
    if m:
        return m.group(1), m.group(2).split(" - ")[0], "dash_pattern"

    return None, None, ""


def extract_company_from_subdir(
    file_path: pathlib.Path,
    source_dir: pathlib.Path | None = None,
) -> tuple[str | None, str | None, str]:
    """Extract company from subdirectory structure.
    
    Returns (company_raw, role_raw, source).
    """
    scan_root = source_dir or RESUMES_DIR
    try:
        rel = file_path.relative_to(scan_root)
    except ValueError:
        return None, None, ""

    parts = rel.parts
    if len(parts) < 2:
        return None, None, ""

    subdir = parts[0]
    
    # Check ambiguous subdir mapping
    if subdir in SUBDIR_COMPANY_MAP:
        company_raw = SUBDIR_COMPANY_MAP[subdir]
        # Role from next subfolder
        if len(parts) >= 3:
            role_raw = parts[1]
        else:
            role_raw = None
        return company_raw, role_raw, f"subdir_map({subdir})"

    # Check if file is a resume-edits.txt — parse header
    if file_path.name == "resume-edits.txt":
        header_company, header_role = parse_resume_edits_header(file_path)
        if header_company and header_role:
            return header_company, header_role, "resume_edits_header"
        # Fallback: company from subdir, role from parent folder
        if len(parts) >= 3:
            return subdir, parts[1], "subdir_role_folder"
        return subdir, None, "subdir"

    # Cover letters in role subfolders
    if len(parts) >= 3:
        role_raw = parts[1]
        return subdir, role_raw, "subdir_role_folder"

    # PDF in company subdir (no role folder)
    return subdir, None, "subdir"


def resolve_role_from_firestore(
    company_slug: str, firestore_apps: dict[str, list[tuple[str, str]]]
) -> tuple[str | None, str, str]:
    """Resolve role from Firestore applications.
    
    Returns (role_slug, confidence_modifier, role_source).
    """
    apps = firestore_apps.get(company_slug, [])
    if not apps:
        return None, "", "none"
    if len(apps) == 1:
        role_raw = apps[0][1]
        if not role_raw or role_raw.lower() == "none":
            return "general", "high", "firestore_single"
        role_slug = contract.normalize_dir_name(role_raw)
        return role_slug, "high", "firestore_single"
    # Multiple roles — ambiguous
    # Pick the first one as a best guess
    role_raw = apps[0][1]
    if not role_raw or role_raw.lower() == "none":
        return "general", "medium", "firestore_ambiguous"
    role_slug = contract.normalize_dir_name(role_raw)
    return role_slug, "medium", f"firestore_ambiguous ({len(apps)} roles)"


def load_firestore_applications() -> dict[str, list[tuple[str, str]]]:
    """Load all applications from Firestore, grouped by companyId.
    
    Returns {companyId: [(company_name, role), ...]}.
    """
    try:
        db = client.getDb()
        apps: dict[str, list[tuple[str, str]]] = {}
        for doc in db.collection(client.APPLICATIONS).stream():
            data = doc.to_dict()
            if not data:
                continue
            company_id = data.get("companyId", "")
            company = data.get("company", "")
            role = data.get("role", "")
            if not company_id:
                continue
            if company_id not in apps:
                apps[company_id] = []
            apps[company_id].append((company, role))
        return apps
    except Exception as e:
        print(f"Warning: Could not load Firestore applications: {e}", file=sys.stderr)
        return {}


def scan_file(
    file_path: pathlib.Path,
    firestore_apps: dict[str, list[tuple[str, str]]],
    source_dir: pathlib.Path | None = None,
) -> dict | None:
    """Scan a single file and extract migration metadata.
    
    Returns a manifest entry dict or None if file should be skipped.
    """
    # Skip .DS_Store and other hidden files
    if file_path.name.startswith("."):
        return None

    company_raw = None
    role_raw = None
    source = ""

    # Strategy 1: Subdirectory-based extraction
    company_raw, role_raw, source = extract_company_from_subdir(file_path, source_dir)

    # Strategy 2: Filename-based extraction (if no subdir match)
    if not company_raw:
        company_raw, role_raw, source = extract_company_from_filename(file_path.name)

    # Handle generic files
    if not company_raw:
        # Check for generic patterns
        stem = pathlib.Path(file_path.name).stem
        if any(pattern in stem for pattern in ["LIB_V", "Res_V", "Resume_20"]):
            date = get_file_date(file_path)
            return {
                "source": str(file_path),
                "company": "_generic",
                "role": "resume",
                "role_source": "generic",
                "date": date,
                "target": f"job-applications/_generic/resume/{date}/{file_path.name}",
                "confidence": "low",
                "in_tracker": False,
                "notes": "Generic resume file — no company detected",
            }
        return None

    # Split camelCase for company name
    company_split = split_camel_case(company_raw)
    company_slug = contract.normalize_dir_name(company_split)

    # Check if the extracted "company" is actually a generic version token
    if company_slug in ("v3", "v2", "v1", "v4"):
        date = get_file_date(file_path)
        return {
            "source": str(file_path),
            "company": "_generic",
            "role": "resume",
            "role_source": "generic",
            "date": date,
            "target": f"job-applications/_generic/resume/{date}/{file_path.name}",
            "confidence": "low",
            "in_tracker": False,
            "notes": "Generic resume file — no company detected",
        }

    if not company_slug:
        return None

    # Check if company is in tracker
    in_tracker = company_slug in firestore_apps

    # Role resolution
    role_source = ""
    confidence = "high"
    notes = ""

    if role_raw:
        # Split camelCase for role too
        role_split = split_camel_case(role_raw)
        role_slug = contract.normalize_dir_name(role_split)
        if not role_slug:
            role_slug = "general"
        role_source = f"filename ({source})"
    else:
        # Try Firestore lookup
        role_slug, conf_mod, role_source = resolve_role_from_firestore(company_slug, firestore_apps)
        if conf_mod == "medium":
            confidence = "medium"
            notes = role_source
        if not role_slug:
            role_slug = "general"
            role_source = "default"
            confidence = "low"
            notes = "No role detected"

    date = get_file_date(file_path)
    target = f"job-applications/{company_slug}/{role_slug}/{date}/{file_path.name}"

    # Check for unresolved cases
    if company_slug == "consumer-tech":
        confidence = "unresolved"
        notes = "Generic placeholder company — user must identify actual company"

    return {
        "source": str(file_path),
        "company": company_slug,
        "role": role_slug,
        "role_source": role_source,
        "date": date,
        "target": target,
        "confidence": confidence,
        "in_tracker": in_tracker,
        "notes": notes,
    }


def scan(source_dir: pathlib.Path | None = None) -> list[dict]:
    """Scan all files in source_dir (defaults to RESUMES_DIR) and return manifest entries."""
    scan_dir = source_dir or RESUMES_DIR
    print(f"Scanning {scan_dir}...")
    
    firestore_apps = load_firestore_applications()
    print(f"  Loaded {len(firestore_apps)} companies from Firestore")

    entries = []
    files_scanned = 0
    
    for file_path in sorted(scan_dir.rglob("*")):
        if not file_path.is_file():
            continue
        if file_path.name == ".DS_Store":
            continue
        
        files_scanned += 1
        entry = scan_file(file_path, firestore_apps, scan_dir)
        if entry:
            entries.append(entry)

    print(f"  Scanned {files_scanned} files, produced {len(entries)} manifest entries")
    return entries


def write_manifest(entries: list[dict], source_dir: pathlib.Path | None = None) -> None:
    """Write manifest JSON and UNRESOLVED.md."""
    # Ensure generated directory exists
    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    
    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        json.dump(entries, f, indent=2, ensure_ascii=False)
    print(f"Manifest written to {MANIFEST_PATH}")

    # Write UNRESOLVED.md
    unresolved = [e for e in entries if e["confidence"] in ("unresolved", "low", "medium")]
    if unresolved:
        lines = [
            "# Unresolved Migration Entries",
            "",
            "These entries need manual review before running `--apply`.",
            "",
            "| Source | Company | Role | Confidence | Notes |",
            "|---|---|---|---|---|",
        ]
        for e in unresolved:
            # Show full relative path from source directory for easy locating
            src_path = pathlib.Path(e["source"])
            if source_dir and str(src_path).startswith(str(source_dir)):
                source_short = str(src_path.relative_to(source_dir))
            else:
                source_short = src_path.name
            lines.append(
                f"| `{source_short}` | {e['company']} | {e['role']} | {e['confidence']} | {e['notes']} |"
            )
        lines.append("")
        with open(UNRESOLVED_PATH, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))
        print(f"Unresolved report written to {UNRESOLVED_PATH} ({len(unresolved)} entries)")
    else:
        print("All entries resolved — no UNRESOLVED.md needed")


def apply(entries: list[dict]) -> None:
    """Apply the manifest: create directories and copy files."""
    print("Applying migration...")
    
    copied = 0
    skipped = 0
    
    for entry in entries:
        # Skip unresolved entries
        if entry["confidence"] == "unresolved":
            skipped += 1
            print(f"  SKIP (unresolved): {pathlib.Path(entry['source']).name}")
            continue

        source = pathlib.Path(entry["source"])
        target = REPO_ROOT / entry["target"]
        
        # Create target directory
        target.parent.mkdir(parents=True, exist_ok=True)
        
        # Copy file
        try:
            shutil.copy2(str(source), str(target))
            copied += 1
            print(f"  COPY: {source.name} → {entry['target']}")
        except (OSError, IOError) as e:
            print(f"  ERROR: {source.name} → {e}", file=sys.stderr)

    print(f"\nMigration complete: {copied} copied, {skipped} skipped")


def upload(entries: list[dict]) -> None:
    """Upload migrated files to GCS."""
    client.assertWritesAllowed("GCS artifact upload")
    
    bucket = client.getBucket()
    db = client.getDb()
    
    print(f"Uploading to GCS bucket: {bucket.name}")
    
    # Build a lookup of Firestore applications by company+role for artifact linking
    firestore_apps: dict[str, list] = {}
    for doc in db.collection(client.APPLICATIONS).stream():
        data = doc.to_dict()
        if not data:
            continue
        company_id = data.get("companyId", "")
        role = data.get("role", "")
        if company_id not in firestore_apps:
            firestore_apps[company_id] = []
        firestore_apps[company_id].append({
            "doc_id": doc.id,
            "role": role,
            "ref": doc.reference,
        })

    uploaded = 0
    skipped = 0
    errors = 0
    
    for entry in entries:
        if entry["confidence"] == "unresolved":
            skipped += 1
            continue

        source = pathlib.Path(entry["source"])
        target = REPO_ROOT / entry["target"]
        
        if not target.exists():
            print(f"  SKIP (not found): {target.name}")
            skipped += 1
            continue

        company_slug = entry["company"]
        role_slug = entry["role"]
        date = entry["date"]
        
        # Determine artifact kind based on file extension
        ext = target.suffix.lower()
        name = target.name.lower()
        
        # Skip intermediate/compilation files
        if ext in (".aux", ".log", ".out", ".cls", ".toc", ".synctex.gz"):
            skipped += 1
            continue
        
        if ext in (".docx", ".pdf", ".gdoc"):
            kind = "CV"
        elif ext == ".md" and "outcome" in name:
            kind = "OUTCOME"
        elif ext == ".txt":
            kind = "COVER_LETTER"
        elif ext == ".html":
            kind = "POSTING"
        elif ext == ".tex":
            kind = "CV"
        elif ext == ".json":
            kind = "POSTING"
        else:
            # Skip unknown file types
            skipped += 1
            continue

        try:
            # Describe the file
            import artifacts
            desc = artifacts.describe(str(target))
            
            # Upload to GCS
            storage, did_upload = artifacts.ensureUpload(
                bucket,
                company=company_slug,
                role=role_slug,
                date=date,
                kind=kind,
                path=str(target),
                sha256=desc["sha256"],
                size=desc["size"],
            )
            
            if did_upload:
                uploaded += 1
                print(f"  UPLOAD: {target.name} → {storage['path']}")
            else:
                print(f"  EXISTS: {target.name} → {storage['path']}")
            
        except Exception as e:
            errors += 1
            print(f"  ERROR: {target.name} → {e}", file=sys.stderr)

    print(f"\nUpload complete: {uploaded} uploaded, {skipped} skipped, {errors} errors")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Migrate untracked resume artifacts into job-applications/ structure"
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--scan", action="store_true", help="Scan and generate manifest (default)")
    mode.add_argument("--apply", action="store_true", help="Apply manifest: create dirs and copy files")
    mode.add_argument("--upload", action="store_true", help="Upload copied files to GCS")
    parser.add_argument(
        "--source-dir",
        type=pathlib.Path,
        default=None,
        help="Source directory to scan (default: /Users/fnc314/Documents/Resumes)",
    )
    parser.add_argument(
        "--manifest",
        type=pathlib.Path,
        default=None,
        help="Manifest file to apply or upload (default: generated/resume-migration-manifest.json)",
    )
    args = parser.parse_args()

    source_dir = args.source_dir or RESUMES_DIR
    manifest_path = args.manifest or MANIFEST_PATH

    if args.apply:
        # Read manifest
        if not manifest_path.exists():
            print(f"Error: Manifest not found: {manifest_path}", file=sys.stderr)
            return 1
        entries = json.loads(manifest_path.read_text(encoding="utf-8"))
        apply(entries)
        return 0

    if args.upload:
        # Read manifest
        if not manifest_path.exists():
            print(f"Error: Manifest not found: {manifest_path}", file=sys.stderr)
            return 1
        entries = json.loads(manifest_path.read_text(encoding="utf-8"))
        upload(entries)
        return 0

    # Default: scan mode
    entries = scan(source_dir)
    write_manifest(entries, source_dir)
    
    # Summary
    high = sum(1 for e in entries if e["confidence"] == "high")
    medium = sum(1 for e in entries if e["confidence"] == "medium")
    low = sum(1 for e in entries if e["confidence"] == "low")
    unresolved = sum(1 for e in entries if e["confidence"] == "unresolved")
    
    print(f"\nSummary:")
    print(f"  High confidence:   {high}")
    print(f"  Medium confidence: {medium}")
    print(f"  Low confidence:    {low}")
    print(f"  Unresolved:        {unresolved}")
    print(f"  Total:             {len(entries)}")
    
    if unresolved:
        print(f"\n⚠ {unresolved} entries need manual resolution in {UNRESOLVED_PATH}")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
