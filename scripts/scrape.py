#!/usr/bin/env python3
"""DEPRECATED: Job portal scraping orchestrator — deferred, spawns external Bun/TypeScript CLIs.

scrape.py — Run portal CLI searches and collect job listings.

Discovers enabled portal skills under .agents/skills/, runs each with the
configured search queries, deduplicates against seen_jobs.json and the
application tracker, then outputs a JSON report to stdout.

This is the mechanical search portion of the /scrape workflow.  The agent
skill (.agents/skills/job-scraper/SKILL.md) adds fit assessment, detail
fetches, referral links, and health checks on top of this output.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import uuid
from datetime import date, datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SCRAPE_COLLECTION = "scraped-postings"
SKILLS_DIR = REPO_ROOT / ".agents" / "skills"
SEEN_FILE = REPO_ROOT / "job-scraper" / "seen_jobs.json"
QUERIES_FILE = REPO_ROOT / ".agents" / "skills" / "job-scraper" / "search-queries.md"

# Portal-specific flag mapping: each portal's keyword flag and any required flags.
PORTAL_FLAGS: dict[str, dict[str, object]] = {
    "freehire-search": {"keyword_flag": "--query", "extra": []},
    "linkedin-search": {"keyword_flag": "--query", "extra": ["--location", "United States"]},
    "jobindex-search": {"keyword_flag": "--query", "extra": []},
    "jobnet-search": {"keyword_flag": "--search-string", "extra": []},
    "jobbank-search": {"keyword_flag": "--key", "extra": []},
    "jobdanmark-search": {"keyword_flag": "--query", "extra": []},
}


def _bun() -> str:
    """Return the bun binary path."""
    # Use shutil.which to find bun on PATH (mise shims it)
    import shutil
    bun_path = shutil.which("bun")
    if bun_path:
        return bun_path
    # Fallback: mise install directory
    try:
        out = subprocess.check_output(
            ["mise", "where", "bun"], stderr=subprocess.DEVNULL
        )
        install_dir = out.decode().strip()
        candidate = str(Path(install_dir) / "bin" / "bun")
        if Path(candidate).exists():
            return candidate
    except (subprocess.CalledProcessError, FileNotFoundError):
        pass
    return "bun"


def _is_enabled(skill_dir: Path) -> bool:
    """Check whether a portal skill is enabled (frontmatter `enabled: false` disables it)."""
    skill_md = skill_dir / "SKILL.md"
    if not skill_md.exists():
        return False
    text = skill_md.read_text(encoding="utf-8")
    # Look for enabled: false in frontmatter
    fm_match = re.search(r"^---\s*\n(.*?)\n---", text, re.DOTALL)
    if fm_match:
        fm = fm_match.group(1)
        if re.search(r"^enabled:\s*false", fm, re.MULTILINE):
            return False
    return True


def _discover_portals() -> list[tuple[str, Path]]:
    """Return list of (name, path) for enabled portal skills with a CLI."""
    portals: list[tuple[str, Path]] = []
    if not SKILLS_DIR.is_dir():
        return portals
    for entry in sorted(SKILLS_DIR.iterdir()):
        if not entry.is_dir():
            continue
        name = entry.name
        # Skip non-portal skills
        if name not in PORTAL_FLAGS:
            continue
        cli_ts = entry / "cli" / "src" / "cli.ts"
        if not cli_ts.exists():
            continue
        if not _is_enabled(entry):
            continue
        portals.append((name, entry))
    return portals


def _load_seen() -> dict[str, dict[str, object]]:
    """Load seen_jobs.json, creating it if missing."""
    if SEEN_FILE.exists():
        try:
            data = json.loads(SEEN_FILE.read_text(encoding="utf-8"))
            return data.get("seen", {})
        except (json.JSONDecodeError, KeyError):
            return {}
    return {}


def _get_db():
    """Return the Firestore database client."""
    scripts_dir = REPO_ROOT / "scripts"
    if str(scripts_dir) not in sys.path:
        sys.path.insert(0, str(scripts_dir))
    import client  # noqa: E402

    return client.getDb()


def _load_seen_from_firestore(db) -> dict[str, dict[str, object]]:
    """Load all scraped postings from Firestore and return a dict keyed by
    normalizedUrl and dedupKey, matching the structure of _load_seen()."""
    seen: dict[str, dict[str, object]] = {}
    try:
        docs = db.collection(SCRAPE_COLLECTION).stream()
        for doc in docs:
            data = doc.to_dict()
            if not data:
                continue
            # Index by normalizedUrl
            url_key = data.get("normalizedUrl", "")
            if url_key:
                seen[url_key] = {
                    "title": data.get("title", ""),
                    "company": data.get("company", ""),
                    "url": data.get("url", ""),
                    "first_seen": data.get("firstScrapedAt", ""),
                    "fit": "unknown",
                    "status": data.get("fitStatus", "NEW"),
                    "portal": data.get("portal", ""),
                }
            # Index by dedupKey
            dedup_key = data.get("dedupKey", "")
            if dedup_key:
                seen[dedup_key] = {
                    "title": data.get("title", ""),
                    "company": data.get("company", ""),
                    "url": data.get("url", ""),
                    "first_seen": data.get("firstScrapedAt", ""),
                    "fit": "unknown",
                    "status": data.get("fitStatus", "NEW"),
                    "portal": data.get("portal", ""),
                }
    except Exception as e:
        print(f"  [warn] Firestore read failed: {e}", file=sys.stderr)
    return seen


def _slugify(s: str) -> str:
    """Convert a string to a kebab-case slug."""
    s = re.sub(r"[^a-z0-9]+", "-", s.lower().strip())
    return s.strip("-")


def _save_seen_to_firestore(db, seen_dict: dict[str, dict[str, object]]) -> int:
    """Upsert scraped postings to Firestore. Returns count of documents written."""
    now = datetime.now(timezone.utc).isoformat()
    written = 0

    # Build reverse index: normalizedUrl -> doc_id and dedupKey -> doc_id
    url_to_id: dict[str, str] = {}
    dedup_to_id: dict[str, str] = {}
    try:
        for doc in db.collection(SCRAPE_COLLECTION).stream():
            data = doc.to_dict()
            if not data:
                continue
            uid = doc.id
            url_key = data.get("normalizedUrl", "")
            if url_key:
                url_to_id[url_key] = uid
            dedup_key = data.get("dedupKey", "")
            if dedup_key:
                dedup_to_id[dedup_key] = uid
    except Exception as e:
        print(f"  [warn] Firestore index read failed: {e}", file=sys.stderr)

    for dedup_key, info in seen_dict.items():
        title = info.get("title", "")
        company = info.get("company", "")
        url = info.get("url", "")
        portal = info.get("portal", "")

        if not title or not company:
            continue

        company_slug = _slugify(company)
        title_slug = _slugify(title)
        posting_dedup_key = f"{company_slug}-{title_slug}"
        normalized_url = _url_key(url) if url else ""

        # Check if this posting already exists
        existing_id = None
        if normalized_url and normalized_url in url_to_id:
            existing_id = url_to_id[normalized_url]
        elif posting_dedup_key and posting_dedup_key in dedup_to_id:
            existing_id = dedup_to_id[posting_dedup_key]

        if existing_id:
            # Re-scrape: increment scrapeCount and update lastScrapedAt
            from google.cloud.firestore import Increment  # noqa: E402

            ref = db.collection(SCRAPE_COLLECTION).document(existing_id)
            ref.set(
                {
                    "lastScrapedAt": now,
                    "updatedAt": now,
                    "scrapeCount": Increment(1),
                },
                merge=True,
            )
            written += 1
        else:
            # New posting
            doc_id = str(uuid.uuid7()) if hasattr(uuid, "uuid7") else str(uuid.uuid4())
            doc = {
                "uuid": doc_id,
                "dedupKey": posting_dedup_key,
                "normalizedUrl": normalized_url,
                "companySlug": company_slug,
                "titleSlug": title_slug,
                "title": title,
                "company": company,
                "url": url,
                "location": info.get("location", ""),
                "portal": portal,
                "searchQuery": "",
                "firstScrapedAt": now,
                "lastScrapedAt": now,
                "scrapeCount": 1,
                "fitStatus": "NEW",
                "createdAt": now,
                "updatedAt": now,
            }
            db.collection(SCRAPE_COLLECTION).document(doc_id).set(doc)
            written += 1

        # Non-blocking company metadata enrichment (RM-005)
        try:
            _enrich_company_in_firestore(db, company_slug, company)
        except Exception as e:
            # Non-blocking: failure to enrich company metadata never aborts job scrape
            pass

    return written


def _enrich_company_in_firestore(db: Any, company_slug: str, company_name: str) -> None:
    """Enriches company metadata in the Firestore `companies` collection."""
    if not db or not company_slug:
        return
    comp_ref = db.collection("companies").document(company_slug)
    snap = comp_ref.get()
    now_iso = datetime.now(timezone.utc).isoformat()
    if snap.exists:
        data = snap.to_dict() or {}
        # Only update if fields are missing
        updates = {}
        if not data.get("companyId"):
            updates["companyId"] = company_slug
        if not data.get("name"):
            updates["name"] = company_name
        if updates:
            updates["updatedAt"] = now_iso
            comp_ref.set(updates, merge=True)
    else:
        comp_ref.set(
            {
                "companyId": company_slug,
                "name": company_name,
                "sector": None,
                "metadata": {},
                "createdAt": now_iso,
                "updatedAt": now_iso,
            },
            merge=True,
        )


def _load_tracker_ids() -> set[str]:
    """Load tracker application IDs (company+role combos) for dedup."""
    ids: set[str] = set()
    try:
        result = subprocess.run(
            [sys.executable, str(REPO_ROOT / "scripts" / "tracker.py"), "list", "--json"],
            capture_output=True,
            text=True,
            timeout=30,
        )
        if result.returncode == 0:
            data = json.loads(result.stdout)
            apps = data.get("documents", data) if isinstance(data, dict) else data
            for app in apps:
                company = app.get("company", "") or ""
                role = app.get("role", "") or app.get("roleType", "") or ""
                if company and role:
                    key = _make_key(company, role)
                    ids.add(key)
    except (subprocess.TimeoutExpired, json.JSONDecodeError, FileNotFoundError):
        pass
    return ids


def _make_key(company: str, title: str) -> str:
    """Normalize a company+title combo for dedup."""
    def normalize(s: str) -> str:
        s = re.sub(r"[^a-z0-9]+", "-", s.lower().strip())
        return s.strip("-")
    return f"{normalize(company)}-{normalize(title)}"


def _load_queries() -> list[str]:
    """Extract search queries from search-queries.md."""
    if not QUERIES_FILE.exists():
        return [
            "Principal Android Engineer remote",
            "Staff Android Engineer remote",
            "Mobile Architect remote",
        ]
    text = QUERIES_FILE.read_text(encoding="utf-8")
    queries: list[str] = []
    # Extract quoted queries from priority categories
    for m in re.finditer(r'"([^"]+)"', text):
        q = m.group(1)
        if len(q) > 3:  # skip very short strings
            queries.append(q)
    return queries if queries else ["Android Engineer remote"]


def _run_search(
    portal: str, cli_path: Path, query: str, limit: int = 20
) -> list[dict[str, object]]:
    """Run a single portal search and return results."""
    flags = PORTAL_FLAGS.get(portal, {"keyword_flag": "--query", "extra": []})
    keyword_flag = flags["keyword_flag"]
    extra: list[str] = flags.get("extra", []) or []

    cmd = [
        _bun(),
        "run",
        str(cli_path / "cli" / "src" / "cli.ts"),
        "search",
        keyword_flag,
        query,
        "--format",
        "json",
        "--limit",
        str(limit),
    ]
    # Append portal-specific extra flags
    cmd.extend(extra)

    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=60,
            cwd=str(REPO_ROOT),
        )
        if result.returncode != 0:
            stderr_msg = result.stderr.strip()
            print(
                f"  [warn] {portal}: exit {result.returncode} — {stderr_msg[:120]}",
                file=sys.stderr,
            )
            return []
        data = json.loads(result.stdout)
        results = data.get("results", [])
        # Tag each result with the portal
        for r in results:
            r.setdefault("portal", portal)
        return results
    except (subprocess.TimeoutExpired, json.JSONDecodeError, FileNotFoundError) as e:
        print(f"  [warn] {portal}: {e}", file=sys.stderr)
        return []


def _url_key(url: str | None) -> str:
    """Normalize a URL for dedup."""
    if not url:
        return ""
    return re.sub(r"[?#].*", "", url.lower().strip())


def run_searches(
    queries: list[str] | None = None,
    limit: int = 20,
    priority: int = 3,
    focus: str | None = None,
    db = None,
) -> dict[str, object]:
    """Run all searches and return the report.

    If db is provided, use Firestore for dedup instead of seen_jobs.json.
    """
    if queries is None:
        queries = _load_queries()

    # If focus is specified, prioritize matching queries
    if focus:
        focus_lower = focus.lower()
        matched = [q for q in queries if focus_lower in q.lower()]
        if matched:
            queries = matched + [q for q in queries if q not in matched]

    # Cap to priority categories (roughly 3 queries per priority level)
    if priority > 0:
        queries = queries[: priority * 3]

    portals = _discover_portals()
    if not portals:
        print("No enabled portal CLIs found.", file=sys.stderr)
        return {
            "portals": [],
            "queries": queries,
            "results": [],
            "portal_stats": {},
            "seen": {},
            "meta": {"total": 0},
        }

    if db is not None:
        seen = _load_seen_from_firestore(db)
    else:
        seen = _load_seen()
    tracker_ids = _load_tracker_ids()
    now = date.today().isoformat()

    all_results: list[dict[str, object]] = []
    portal_stats: dict[str, dict[str, int]] = {}

    for portal_name, portal_path in portals:
        portal_stats[portal_name] = {"total": 0, "new": 0, "skipped": 0, "errors": 0}

        for query in queries:
            results = _run_search(portal_name, portal_path, query, limit)
            portal_stats[portal_name]["total"] += len(results)

            for job in results:
                url = job.get("url", "")
                company = job.get("company", "") or ""
                title = job.get("title", "") or ""
                url_k = _url_key(url)
                ct_key = _make_key(company, title) if company and title else ""

                # Dedup: skip if already seen or in tracker
                already_seen = False
                if url_k and url_k in seen:
                    already_seen = True
                if ct_key and ct_key in seen:
                    already_seen = True
                if ct_key and ct_key in tracker_ids:
                    already_seen = True

                if already_seen:
                    portal_stats[portal_name]["skipped"] += 1
                    continue

                # Mark as seen for this run
                dedup_key = url_k or ct_key
                if dedup_key:
                    seen[dedup_key] = {
                        "title": title,
                        "company": company,
                        "url": url,
                        "first_seen": now,
                        "fit": "unknown",
                        "status": "new",
                        "portal": portal_name,
                    }

                job.setdefault("portal", portal_name)
                job.setdefault("first_seen", now)
                all_results.append(job)
                portal_stats[portal_name]["new"] += 1

    return {
        "portals": [p[0] for p in portals],
        "queries": queries,
        "results": all_results,
        "portal_stats": portal_stats,
        "seen": seen,
        "meta": {
            "total": len(all_results),
            "timestamp": datetime.now().isoformat(),
            "date": now,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Run portal CLI searches and collect job listings"
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=20,
        help="Max results per portal per query (default: 20)",
    )
    parser.add_argument(
        "--priority",
        type=int,
        default=3,
        help="Number of priority categories to run (default: 3, 0 = all)",
    )
    parser.add_argument(
        "--focus",
        type=str,
        default=None,
        help="Focus area to prioritize queries (e.g. 'Android')",
    )
    parser.add_argument(
        "--query",
        "-q",
        action="append",
        default=None,
        help="Explicit query (repeatable); overrides search-queries.md",
    )
    parser.add_argument(
        "--no-save-seen",
        action="store_true",
        help="Skip writing seen postings to Firestore (default: saves to Firestore)",
    )
    parser.add_argument(
        "--migrate-seen",
        action="store_true",
        help="Seed existing seen_jobs.json into Firestore, then exit",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Output as JSON instead of a Markdown table",
    )
    parser.add_argument(
        "--summary",
        action="store_true",
        help="Only show summary stats, not individual results",
    )
    parser.add_argument(
        "--health",
        action="store_true",
        help="Only check portal health (probe each portal)",
    )
    args = parser.parse_args()

    queries = args.query if args.query else None

    if args.health:
        # Health check: probe each portal
        portals = _discover_portals()
        print("Portal health check:", file=sys.stderr)
        for portal_name, portal_path in portals:
            cli_ts = portal_path / "cli" / "src" / "cli.ts"
            results = _run_search(portal_name, portal_path, "engineer", limit=3)
            if results:
                print(f"  {portal_name}: healthy ({len(results)} results)", file=sys.stderr)
            else:
                print(f"  {portal_name}: no results (may be degraded)", file=sys.stderr)
        return 0

    # Migrate existing seen_jobs.json to Firestore
    if args.migrate_seen:
        db = _get_db()
        seen = _load_seen()
        if not seen:
            print("No entries in seen_jobs.json to migrate.", file=sys.stderr)
            return 0
        count = _save_seen_to_firestore(db, seen)
        print(f"Migrated {count} entries from seen_jobs.json to Firestore.", file=sys.stderr)
        return 0

    # Initialize Firestore DB by default (skip only with --no-save-seen)
    db = None
    if not args.no_save_seen:
        try:
            db = _get_db()
        except Exception as e:
            print(f"  [warn] Firestore unavailable: {e}. Falling back to file-based mode.", file=sys.stderr)
            db = None

    report = run_searches(queries=queries, limit=args.limit, priority=args.priority, focus=args.focus, db=db)

    # Save seen postings to Firestore by default (skip only with --no-save-seen)
    if not args.no_save_seen and db is not None:
        count = _save_seen_to_firestore(db, report.get("seen", {}))
        print(f"Saved {count} postings to Firestore.", file=sys.stderr)

    if args.json:
        # JSON output (opt-in)
        output = {
            "portals": report["portals"],
            "queries": report["queries"],
            "results": report["results"],
            "portal_stats": report.get("portal_stats", {}),
            "meta": report["meta"],
        }
        print(json.dumps(output, indent=2, ensure_ascii=False))
        return 0

    if args.summary:
        # Summary only
        stats = report.get("portal_stats", {})
        total = report["meta"]["total"]
        print(f"Found {total} new jobs across {len(report['portals'])} portals.")
        for portal, st in stats.items():
            print(f"  {portal}: {st['new']} new, {st['skipped']} skipped, {st['total']} total")
        return 0

    # Default: Markdown table output
    results = report["results"]
    if not results:
        print("No new jobs found.")
        return 0
    print(f"\n## New Job Matches - {report['meta']['date']}")
    print(f"\nFound {report['meta']['total']} new positions across {len(report['portals'])} portals.\n")
    print("| # | Title | Company | Location | Portal | URL |")
    print("|---|-------|---------|----------|--------|-----|")
    for i, job in enumerate(results, 1):
        title = (job.get("title") or "").replace("|", "\\|")
        company = (job.get("company") or "").replace("|", "\\|")
        location = (job.get("location") or "").replace("|", "\\|")
        portal = job.get("portal", "")
        url = job.get("url", "")
        url_cell = f"[Link]({url})" if url else ""
        print(f"| {i} | {title} | {company} | {location} | {portal} | {url_cell} |")
    return 0


if __name__ == "__main__":
    sys.exit(main())
