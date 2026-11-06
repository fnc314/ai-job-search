#!/usr/bin/env python3
"""rank.py — Lightweight single-job fit evaluation CLI.

Accepts a required URL or JD argument, evaluates fit against the candidate
profile, and outputs a markdown summary (default) or JSON (--json).

This is a non-agent, immediate-output version of the /rank workflow focused
on single-job evaluation rather than batch scoring.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
PROFILE_FILE = REPO_ROOT / ".claude" / "skills" / "job-application-assistant" / "01-candidate-profile.md"
EVAL_FILE = REPO_ROOT / ".claude" / "skills" / "job-application-assistant" / "04-job-evaluation.md"

# Scoring weights matching the /rank workflow
WEIGHTS = {
    "technical": 0.30,
    "experience": 0.25,
    "behavioral": 0.15,
    "career": 0.30,
}

# Strong match keywords from the candidate profile
STRONG_TECH_KEYWORDS = [
    "kotlin", "android", "jetpack compose", "kmp", "kotlin multiplatform",
    "gradle", "mobile architecture", "mobile architect", "devex",
    "technical leadership", "staff", "principal", "tech lead",
    "mobile payments", "fido", "mfa", "samsung knox",
    "ci/cd", "jenkins", "github actions", "fastlane",
    "clean architecture", "mvi", "coroutines", "flow",
]

MODERATE_TECH_KEYWORDS = [
    "react native", "firebase", "gcp", "google cloud",
    "javascript", "typescript", "web development",
    "security", "authentication",
]

WEAK_TECH_KEYWORDS = [
    "swift", "objective-c", "ios", "embedded", "data engineering",
    "flutter", "dart",
]

# Experience keywords
EXPERIENCE_KEYWORDS = [
    "enterprise", "financial services", "fintech", "finserv",
    "banking", "payments", "mobile banking",
    "large scale", "scale", "3m+", "10m+", "millions",
    "modular", "multi-module", "build optimization",
    "technical debt", "refactor", "migration",
    "mentoring", "mentorship", "team lead", "architect",
    "cross-functional", "stakeholder",
]

# Behavioral keywords
BEHAVIORAL_KEYWORDS = [
    "best practice", "standard", "process", "governance",
    "documentation", "knowledge sharing",
    "autonomous", "independent", "ownership",
    "modernization", "transformation", "redesign",
    "developer experience", "devex", "tooling",
]

# Career alignment keywords
CAREER_KEYWORDS = [
    "staff", "principal", "lead architect", "mobile architect",
    "technical strategy", "technical direction",
    "fintech", "finserv", "financial services", "banking",
    "remote", "pittsburgh",
    "modernization", "platform",
]

# Deal-breaker patterns
DEAL_BREAKERS = {
    "consulting": [
        "consulting", "consultant", "consultancy", "management consulting",
        "staffing agency", "staff augmentation",
    ],
    "hybrid_stack": [
        "react native", "flutter", "hybrid mobile", "cross-platform framework",
    ],
    "people_management": [
        "people manager", "people management", "direct reports",
        "vp of engineering", "engineering manager", "director of engineering",
        "head of engineering", "cto", "cfo",
    ],
}


def _slugify(s: str) -> str:
    """Convert a string to a kebab-case slug."""
    s = re.sub(r"[^a-z0-9]+", "-", s.lower().strip())
    return s.strip("-")


def _score_dimension(text: str, keywords: list[str]) -> float:
    """Score a dimension based on keyword matches in the job text."""
    if not keywords or not text:
        return 0.0
    text_lower = text.lower()
    matches = sum(1 for kw in keywords if kw.lower() in text_lower)
    # Normalize: cap at len(keywords) * 0.5 to avoid over-scoring
    score = min(matches / max(len(keywords) * 0.3, 1), 1.0)
    return round(score * 100)


def _check_deal_breakers(text: str) -> list[str]:
    """Check for deal-breaker patterns in the job text."""
    text_lower = text.lower()
    found = []
    for category, patterns in DEAL_BREAKERS.items():
        for pattern in patterns:
            if pattern.lower() in text_lower:
                found.append(category)
                break
    return found


def _extract_job_info(text: str) -> dict:
    """Try to extract company and role from job text."""
    company = ""
    role = ""

    lines = text.strip().split("\n")
    for line in lines[:10]:
        line_stripped = line.strip()
        if not line_stripped:
            continue
        if " at " in line_stripped:
            parts = line_stripped.split(" at ", 1)
            role = parts[0].strip()
            company = parts[1].strip()
            break
        elif " - " in line_stripped:
            parts = line_stripped.split(" - ", 1)
            company = parts[0].strip()
            role = parts[1].strip()
            break

    return {"company": company, "role": role}


def _fetch_url_content(url: str) -> str:
    """Try to fetch content from a URL using curl."""
    try:
        result = subprocess.run(
            ["curl", "-sL", "--max-time", "10", "-A", "Mozilla/5.0", url],
            capture_output=True,
            text=True,
            timeout=15,
        )
        if result.returncode == 0 and result.stdout:
            # Strip HTML tags for basic text extraction
            text = re.sub(r"<[^>]+>", " ", result.stdout)
            text = re.sub(r"\s+", " ", text).strip()
            return text[:10000]  # Limit to first 10k chars
    except (subprocess.TimeoutExpired, FileNotFoundError):
        pass
    return ""


def _check_tracker(company: str, role: str) -> dict | None:
    """Check if the job already exists in the tracker."""
    if not company or not role:
        return None
    try:
        result = subprocess.run(
            [sys.executable, str(REPO_ROOT / "scripts" / "tracker.py"), "list", "--json"],
            capture_output=True,
            text=True,
            timeout=30,
        )
        if result.returncode != 0:
            return None
        data = json.loads(result.stdout)
        target = f"{_slugify(company)}-{_slugify(role)}"
        for app in data.get("documents", []):
            app_company = app.get("company", "") or ""
            app_role = app.get("role", "") or app.get("roleType", "") or ""
            app_key = f"{_slugify(app_company)}-{_slugify(app_role)}"
            if app_key == target:
                return app
    except (subprocess.TimeoutExpired, json.JSONDecodeError, FileNotFoundError):
        pass
    return None


def evaluate(text: str) -> dict:
    """Evaluate a job description and return scoring results."""
    technical_score = _score_dimension(text, STRONG_TECH_KEYWORDS)
    experience_score = _score_dimension(text, EXPERIENCE_KEYWORDS)
    behavioral_score = _score_dimension(text, BEHAVIORAL_KEYWORDS)
    career_score = _score_dimension(text, CAREER_KEYWORDS)

    # Check for weak-match keywords that should reduce the score
    weak_matches = [kw for kw in WEAK_TECH_KEYWORDS if kw.lower() in text.lower()]
    if weak_matches:
        technical_score = max(0, technical_score - len(weak_matches) * 10)

    # Weighted overall score
    overall = (
        technical_score * WEIGHTS["technical"]
        + experience_score * WEIGHTS["experience"]
        + behavioral_score * WEIGHTS["behavioral"]
        + career_score * WEIGHTS["career"]
    )
    overall = round(overall)

    # Verdict band
    if overall >= 85:
        verdict = "Strong Fit"
    elif overall >= 70:
        verdict = "Good Fit"
    elif overall >= 55:
        verdict = "Moderate Fit"
    elif overall >= 40:
        verdict = "Weak Fit"
    else:
        verdict = "Poor Fit"

    deal_breakers = _check_deal_breakers(text)
    job_info = _extract_job_info(text)

    # Strengths and gaps
    strengths = []
    gaps = []

    if technical_score >= 60:
        strengths.append("Strong technical alignment with Android/Kotlin expertise")
    else:
        gaps.append("Limited technical keyword match — may not be Android-focused")

    if experience_score >= 60:
        strengths.append("Experience requirements align with enterprise/mobile background")
    else:
        gaps.append("Experience requirements may not match enterprise/mobile focus")

    if behavioral_score >= 60:
        strengths.append("Cultural fit — aligns with process-driven, autonomous work style")
    else:
        gaps.append("Behavioral fit unclear — may not emphasize autonomy or process improvement")

    if career_score >= 60:
        strengths.append("Career alignment — matches target role level and sector")
    else:
        gaps.append("Career alignment weak — role level or sector may not match targets")

    if weak_matches:
        gaps.append(f"Weak-match technologies detected: {', '.join(weak_matches)}")

    for db in deal_breakers:
        if db == "consulting":
            gaps.append("DEAL-BREAKER: appears to be a consulting role")
        elif db == "hybrid_stack":
            gaps.append("DEAL-BREAKER: primary focus on hybrid mobile stack (React Native/Flutter)")
        elif db == "people_management":
            gaps.append("DEAL-BREAKER: appears to be a people management role")

    return {
        "overall": overall,
        "verdict": verdict,
        "technical": technical_score,
        "experience": experience_score,
        "behavioral": behavioral_score,
        "career": career_score,
        "deal_breakers": deal_breakers,
        "strengths": strengths,
        "gaps": gaps,
        "company": job_info.get("company", ""),
        "role": job_info.get("role", ""),
    }


def format_markdown(result: dict, tracker_record: dict | None) -> str:
    """Format results as a markdown summary."""
    lines = []
    lines.append(f"## Job Fit Evaluation")
    lines.append("")

    if result["company"] and result["role"]:
        lines.append(f"**{result['company']} — {result['role']}**")
        lines.append("")

    lines.append(f"**Overall Score:** {result['overall']}/100 — {result['verdict']}")
    lines.append("")

    lines.append("### Dimension Scores")
    lines.append("")
    lines.append(f"- **Technical (30%):** {result['technical']}")
    lines.append(f"- **Experience (25%):** {result['experience']}")
    lines.append(f"- **Behavioral (15%):** {result['behavioral']}")
    lines.append(f"- **Career (30%):** {result['career']}")
    lines.append("")

    if result["strengths"]:
        lines.append("### Strengths")
        lines.append("")
        for s in result["strengths"]:
            lines.append(f"- {s}")
        lines.append("")

    if result["gaps"]:
        lines.append("### Gaps & Concerns")
        lines.append("")
        for g in result["gaps"]:
            lines.append(f"- {g}")
        lines.append("")

    if tracker_record:
        lines.append("### Tracker Status")
        lines.append("")
        app_id = tracker_record.get("id", "unknown")
        app_status = tracker_record.get("status", "unknown")
        lines.append(f"This job already exists in the tracker: `{app_id}` (Status: {app_status})")
        lines.append("")

    return "\n".join(lines)


def _read_file_content(path: str) -> str:
    """Read job description text from a local file."""
    file_path = Path(path)
    if not file_path.is_file():
        print(f"File not found: {path}", file=sys.stderr)
        return ""
    return file_path.read_text(encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Evaluate a job posting against the candidate profile"
    )
    parser.add_argument(
        "url_or_jd",
        help="Job URL, local file path, or job description text",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Output as JSON instead of markdown",
    )
    args = parser.parse_args()

    # Get job text
    if args.url_or_jd.startswith("http"):
        text = _fetch_url_content(args.url_or_jd)
        if not text:
            print(f"Could not fetch content from URL: {args.url_or_jd}", file=sys.stderr)
            print("Try providing a local file path or the job description text directly.", file=sys.stderr)
            return 1
    elif Path(args.url_or_jd).is_file():
        text = _read_file_content(args.url_or_jd)
        if not text:
            return 1
    elif "/" in args.url_or_jd or "\\" in args.url_or_jd or Path(args.url_or_jd).suffix:
        print(f"File not found: {args.url_or_jd}", file=sys.stderr)
        return 1
    else:
        text = args.url_or_jd

    result = evaluate(text)

    # Check tracker
    tracker_record = _check_tracker(result["company"], result["role"])

    if args.json:
        output = {**result, "tracker": tracker_record}
        print(json.dumps(output, indent=2, ensure_ascii=False))
    else:
        print(format_markdown(result, tracker_record))

    return 0


if __name__ == "__main__":
    sys.exit(main())
