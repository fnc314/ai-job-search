# Implementation Plan: Pure Kotlin Migration of URL Cleaning (RM-001 & RM-003)

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `records/parser`, `scripts/urls.py`, `scripts/tests/test_urls.py`, `records-parser-jvm`, `BackendService`  
**Evaluates:** Roadmap item RM-001 (Kotlin-Only Migration, 35% -> 45%) and RM-003 (Backend Functionality Abstraction)  
**Schema Authority:** `records-proto` (`dev.fnc314.tools.jobsearch.tracker.v1`)  

---

## 1. Executive Summary

A review of [`ROADMAP.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ROADMAP.md) shows that **RM-005** (LinkedIn Company Enrichment) was completed today (100%), leaving **RM-001** (Complete Kotlin-Only Migration, 35%), **RM-002** (AI-Portable Pluginization, 0%), **RM-003** (Backend Functionality Abstraction, 75%), and **RM-004** (Subcollection vs Collection Events Investigation, 0%).

The most immediate, high-value, and deterministic next action item is the migration of **`scripts/urls.py`** to pure Kotlin in `records/parser` as **`UrlSanitizer.kt`**.
URL sanitization is a fundamental building block invoked across URL ingest, posting scraping, and tracking. It has a comprehensive, self-contained unit test suite (`scripts/tests/test_urls.py`), zero heavy third-party dependencies, and directly advances both **RM-001** and **RM-003**.

---

## 2. Problem Statement & Motivation

Currently, URL cleaning is implemented in Python in [`scripts/urls.py`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/scripts/urls.py):
- Strips 40+ tracking parameters (`utm_*`, `fbclid`, `gclid`, `trk`, `refId`, `trackingId`, `gh_src`, etc.).
- Normalizes URL schemes, netloc, path, query strings, and fragments.
- Resolves redirects via HTTP HEAD/GET (`clean_and_resolve_url`).

Because `records` and `backend` (Cloud Run) are written in Kotlin, maintaining this logic in Python creates runtime fragmentation. Ingesting job postings or links directly into Kotlin backend services or frontend apps requires either a duplicate implementation or an unnecessary RPC/CLI hop back to Python.

Migrating this to `records/parser` provides:
1. Pure Multiplatform / JVM Kotlin implementation (`dev.fnc314.tools.jobsearch.parser.UrlSanitizer`).
2. Exact test-suite parity with `scripts/tests/test_urls.py` ported to Kotlin test (`UrlSanitizerTest.kt`).
3. JPype bridge and CLI export so Python callers can seamlessly delegate to the Kotlin implementation during the transitional period.
4. Deprecation of `scripts/urls.py` with an automatic fallback/bridge to Kotlin.

---

## 3. Technical Design

### 3.1. Target Architecture: `records/parser`

Create `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/UrlSanitizer.kt`:
```kotlin
package dev.fnc314.tools.jobsearch.parser

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UrlSanitizer {
    private val TRACKING_PARAMS: Set<String> = setOf(
        // Google / Analytics
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "gclid", "gclsrc", "dclid", "_ga", "_gl",
        // Facebook / Meta
        "fbclid", "fbadid",
        // LinkedIn
        "trk", "trkInfo", "refId", "trackingId", "midToken", "eid", "otpToken",
        // Twitter / X
        "twclid",
        // General / Hubspot / Marketo / Mailchimp
        "mc_cid", "mc_eid", "mkt_tok", "_hsenc", "_hsmi", "hsCtaTracking",
        // Recruiting / ATS Specific
        "gh_src", "gh_jid", "src", "source", "iis", "iisn"
    )

    fun cleanUrl(rawUrl: String): String { ... }
}
```

### 3.2. Redirect Resolution (`cleanAndResolveUrl`)
For resolving redirects:
- Implement in Kotlin using `java.net.http.HttpClient` (JVM) following redirect chains up to `maxRedirects = 5` and inspecting `Location` headers.
- Return the sanitized destination URL.

### 3.3. Test Suite Parity
Port all test cases from `scripts/tests/test_urls.py` to:
`records/parser/src/test/kotlin/dev/fnc314/tools/jobsearch/parser/UrlSanitizerTest.kt`
- Test basic tracking parameter stripping (`utm_*`, `trk`, `fbclid`, etc.).
- Test preservation of non-tracking parameters (`id`, `page`, `view`, etc.).
- Test handling of URLs with fragments, invalid URLs, empty strings, and complex query parameters.
- Verify 100% equivalence with Python outputs.

### 3.4. Python Bridge & CLI
Expose `UrlSanitizer.cleanUrl` via `JPypeBridge.kt` (or fallback CLI) and update `scripts/urls.py` to route through the Kotlin implementation when available, issuing a deprecation notice.

---

## 4. Verification Plan

1. **Gradle Compilation & Tests:**
   ```bash
   ./gradlew :records:parser:test
   ```
2. **Python Bridge Verification:**
   ```bash
   python3 -m unittest scripts/tests/test_urls.py
   ```
3. **Roadmap Update:**
   Update [`ROADMAP.md`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ROADMAP.md) to record the migration in RM-001 progress log and update completion metric.
