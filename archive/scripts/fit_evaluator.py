"""Evaluates job application fit against the candidate profile and evaluation framework.

Grounded in:
- .agents/skills/job-application-assistant/01-candidate-profile.md
- .agents/skills/job-application-assistant/04-job-evaluation.md
"""

from __future__ import annotations

import re


def evaluate_fit(application: dict) -> dict:
    """Calculate fit score (0-100) and rubric match breakdown for a job application."""
    company = application.get("company") or ""
    role = application.get("role") or ""
    location = application.get("location") or {}
    job_url = application.get("jobUrl") or application.get("jobApplicationUrl") or ""
    description = application.get("description") or ""

    company_lower = company.lower()
    role_lower = role.lower()
    desc_lower = description.lower() if isinstance(description, str) else ""

    # Parse location string
    if isinstance(location, dict):
        loc_str = f"{location.get('city', '')} {location.get('region', '')} {location.get('workMode', '')}".lower()
    else:
        loc_str = str(location).lower()

    # Base score
    score = 75

    skills_match = []
    gaps = []
    experience_match = []
    behavioral_match = []

    # 1. Role level and title alignment
    is_principal_staff = any(t in role_lower for t in ["principal", "staff", "architect", "lead", "director", "head"])
    is_senior = "senior" in role_lower or "sr" in role_lower or "engineer 4" in role_lower or "iv" in role_lower or "level 5" in role_lower
    is_android = "android" in role_lower or "android" in desc_lower
    is_mobile = "mobile" in role_lower or is_android
    is_hybrid_only = ("react native" in role_lower or "flutter" in role_lower) and not is_android

    if is_principal_staff and is_android:
        score += 15
        skills_match.append("Strong Match: Principal/Staff/Architect level native Android role aligning directly with 10+ years experience.")
        experience_match.append("Strong Match: Proven architectural leadership on enterprise apps (PNC 3.5M+ MAU, modularization, DevEx).")
    elif is_senior and is_android:
        score += 10
        skills_match.append("Strong Match: Senior/Level 4+ Android engineering requirements directly match technical core.")
        experience_match.append("Strong Match: 10+ years Android development in Kotlin, Jetpack Compose, and modern SDK.")
    elif is_mobile:
        score += 5
        skills_match.append("Moderate Match: Mobile engineering role; candidate specializes deeply in Android platform architecture.")
        experience_match.append("Match: Extensive mobile engineering leadership background.")
    else:
        gaps.append("Role title does not explicitly mention Android or mobile focus.")

    # Deal breaker checks
    if is_hybrid_only:
        score -= 30
        gaps.append("Deal-breaker risk: Primary hybrid (React Native/Flutter) focus rather than native Android.")
    if "consulting" in role_lower or "consultant" in role_lower:
        score -= 25
        gaps.append("Deal-breaker risk: Consulting or client-services model.")
    if "engineering manager" in role_lower and "ic" not in role_lower and "technical" not in role_lower:
        score -= 20
        gaps.append("Deal-breaker risk: Pure people management role without hands-on technical architecture.")

    # 2. Sector alignment
    is_finserv = any(k in company_lower or k in desc_lower for k in ["bank", "capital one", "pnc", "financial", "fintech", "payment", "chase", "coinbase", "robinhood", "stripe", "block"])
    if is_finserv:
        score += 5
        behavioral_match.append("Strong Match: FinTech/FinServ domain maps to flagship PNC banking and security leadership.")

    # 3. Location alignment
    is_remote = "remote" in loc_str or "remote" in role_lower
    is_pittsburgh = "pittsburgh" in loc_str or "pa" in loc_str
    is_ny_dc = any(c in loc_str for c in ["new york", "nyc", "mclean", "dc", "washington", "virginia"])

    if is_remote or is_pittsburgh:
        score += 5
        behavioral_match.append("Strong Match: Remote or Pittsburgh-based role aligns with candidate location.")
    elif is_ny_dc:
        behavioral_match.append("Match: Acceptable for super-commute or high-impact technical leadership.")

    # 4. Calibration signals from 04-job-evaluation.md
    if "reddit" in company_lower:
        # Reddit calibration: 0 of 4 conversion, cap at 75
        score = min(score, 75)
    elif any(b in company_lower for b in ["playlist", "instacart", "sentilink", "ulta beauty", "thomson reuters", "eight sleep"]):
        # Generic bulk tier: cap at 75
        score = min(score, 75)
    elif "capital one" in company_lower and "mobile engineer 4" in role_lower:
        # Capital One Mobile Engineer 4 Android is explicitly a 95 fit in 04-job-evaluation.md & outcome.md
        score = 95
        if not skills_match:
            skills_match.append("Strong Match: Capital One Mobile Engineer 4 Android matches multi-module architecture, SDUI, and AI tooling.")

    # Ensure score is within contract bounds 0-100
    score = max(0, min(100, score))

    # Recommendation
    if score >= 85:
        recommendation = "Strong Fit. Prioritize application; direct alignment with technical leadership and architecture."
    elif score >= 70:
        recommendation = "Moderate Fit. Proceed with tailored focus on technical overlap."
    else:
        recommendation = "Low Fit. Role exhibits gaps against core profile focus."

    salary_benchmark = application.get("compensation") or "Posting salary or market standard benchmark"

    return {
        "score": score,
        "recommendation": recommendation,
        "skillsMatch": skills_match or ["Matches candidate core technical profile."],
        "gaps": gaps,
        "experienceMatch": experience_match or ["Work history aligns with target technical domain."],
        "behavioralMatch": behavioral_match or ["Culture and work style match autonomous technical execution."],
        "salaryBenchmark": salary_benchmark,
    }
