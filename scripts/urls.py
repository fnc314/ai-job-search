"""URL sanitation utility: strip tracking and telemetry query parameters."""

from __future__ import annotations

import urllib.parse

# Case-insensitive set of query parameter keys known to be tracking / telemetry
TRACKING_PARAMS = {
    # Google Analytics / Urchin Traffic Monitor
    "utm_source",
    "utm_medium",
    "utm_campaign",
    "utm_term",
    "utm_content",
    "utm_id",
    "utm_source_platform",
    "utm_creative_format",
    "utm_marketing_tactic",
    "utm_name",
    # Ad platforms / Click IDs
    "fbclid",
    "gclid",
    "gclsrc",
    "dclid",
    "wbraid",
    "gbraid",
    "msclkid",
    "twclid",
    "ttclid",
    "sc_clickid",
    "yclid",
    "li_fat_id",
    "rb_clickid",
    "wickedid",
    # Job board / ATS tracking & referral sources
    "gh_src",       # Greenhouse tracking source (keep gh_jid!)
    "trk",          # LinkedIn tracking
    "trkinfo",      # LinkedIn tracking info
    "trackingid",   # Common ATS tracking ID
    "midtoken",     # LinkedIn email tracking token
    "trkemail",     # LinkedIn email tracking
    "ref",          # Generic referrer
    "ref_id",
    "ref_src",
    "referrer",
    "original_referer",
    "from_page",
    "cm_mmc",
    # Email / Marketing automation
    "mc_cid",       # Mailchimp campaign ID
    "mc_eid",       # Mailchimp email ID
    "_hsenc",       # HubSpot
    "_hsmi",        # HubSpot
    "vero_id",
    "mkt_tok",      # Marketo tracking token
    # Social media share IDs
    "si",           # Spotify / YouTube share tracker
    "igshid",       # Instagram share ID
    "share_id",
}


def clean_url(raw_url: str | None) -> str | None:
    """Strip known tracking and telemetry query parameters from a URL.

    Preserves functional query parameters (e.g. gh_jid for Greenhouse job IDs).
    Removes trailing '?' when all query parameters are stripped.
    Returns None or empty string if input is None or empty.
    """
    if raw_url is None:
        return None
    url = raw_url.strip()
    if not url:
        return ""

    parsed = urllib.parse.urlsplit(url)
    if not parsed.scheme and not parsed.netloc:
        # Not a complete URL or relative path/filename
        return url

    # Parse query parameters (keep_blank_values=False drops empty params)
    query_params = urllib.parse.parse_qsl(parsed.query, keep_blank_values=False)
    cleaned_params = [
        (k, v) for (k, v) in query_params
        if k.lower() not in TRACKING_PARAMS and not k.lower().startswith("utm_")
    ]

    new_query = urllib.parse.urlencode(cleaned_params)

    # Reconstruct the URL without empty query or trailing '?'
    cleaned = urllib.parse.urlunsplit((
        parsed.scheme,
        parsed.netloc,
        parsed.path,
        new_query,
        parsed.fragment
    ))
    return cleaned
