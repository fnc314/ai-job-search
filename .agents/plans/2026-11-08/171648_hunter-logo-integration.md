# Implementation Plan: Hunter.io Logo Integration

**Status:** Completed  
**Date:** 2026-11-08  
**Target:** `records` and `ui/kmp` (cross-subsystem), tracker, ui/terminal  
**Model:** Junie  
**Session:** session-261108-171648-1mrg  
**Depends On:** [`add-coil-version-catalog.md`](../../../ui/kmp/.agents/plans/2026-11-08/173039_add-coil-version-catalog.md)  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  

---

### Reviewed and Revised by Gemini 3.8 Flash in 9f99039c-c041-4d65-9682-2de5043c14ed on 2026-11-08 at 17:35:00

## 1. Executive Summary & Goals

Integrate Hunter.io's free, public Company Logo API as a third-party source for company logos. Store logo metadata (URL, format, resolution, fetch timestamp) in the existing `companies` Firestore collection — **not** the image blob. Wire the logo rendering into `ui/kmp` (mobile/web dashboard via Coil 3 Multiplatform) and `ui/terminal` (curses TUI with inline image rendering using the Kitty Graphics Protocol in Ghostty, falling back to OSC 8 clickable terminal hyperlinks).

**User Stories:**
- As a user viewing the KMP app, I want to see the company logo next to the company name in application detail and list views so I can quickly recognize the employer.
- As a user viewing the curses TUI in Ghostty, I want to see the company logo rendered inline via the Kitty graphics protocol, with an OSC 8 clickable link fallback.
- As an admin, I want to run `mise run tracker logo-fetch` to discover, validate, and populate logo metadata for all companies in one safe, idempotent command.

**Scope:**

**In Scope:**
- Extend the `Company` proto, `tracker.schema.json`, and KMP model with logo metadata fields.
- Add a `tracker logo-fetch` CLI subcommand that extracts registered domains for existing companies, validates availability via `https://logos.hunter.io/{domain}`, and upserts metadata into Firestore.
- Register `logo-fetch` in `scripts/specs/tracker.usage.kdl` to satisfy pre-commit completion and doc generation tasks.
- Add Coil 3 Multiplatform to `ui/kmp/gradle/libs.versions.toml` and wire `AsyncImage` into `JobApplicationDetailsScreen.kt`.
- Wire Ghostty Kitty Graphics Protocol rendering and OSC 8 clickable links into `tracker_tui.py`.
- Unit tests mocking HTTP responses so test suites run cleanly inside sandboxed and offline environments.

**Out of Scope:**
- Storing or caching binary image blobs in Firestore or GCS.
- Manual logo uploading/editing UI.
- Other paid enrichment services (Clearbit, Apollo, Brandfetch).

---

## 2. Architecture & Design Principles

### Hunter.io Logo API Specifications
- **Endpoint:** `https://logos.hunter.io/{domain}`
- **Authentication:** None required (free, public endpoint; modern drop-in replacement for deprecated legacy Clearbit endpoints).
- **Return Format:** Raw binary image file (`content-type: image/avif`, `image/webp`, or `image/png`).
- **HTTP Status Codes:** `200 OK` if found; `404 Not Found` if missing (Hunter automatically enqueues missing domains for discovery).
- **No Query Sizing:** Hunter serves high-resolution vector/raster square logos directly; scaling is handled client-side.

### Domain Extraction & Normalization
Application records store company website URLs in varying formats (e.g., `https://www.affirm.com/careers`, `http://stripe.com`, or plain `pnc`). The `logo-fetch` tool normalizes targets before making HTTP requests:
1. Parse hostname with `urllib.parse.urlparse`.
2. Strip leading `www.` and path segments.
3. If no URL is recorded, fallback to `{companyId}.com` or lookup via known aliases.

### Image Consumption by Subsystem
1. **Kotlin Multiplatform UI (`ui/kmp`):**
   - Configured with Coil 3 Multiplatform (`io.coil-kt.coil3:coil-compose` and `coil-network-ktor3`).
   - Uses `AsyncImage` with circular/rounded-corner clipping, placeholder avatars (company initial), and failure fallbacks.
2. **Terminal UI (`ui/terminal`):**
   - **Primary (Ghostty):** Kitty Graphics Protocol inline rendering (`\x1b_Ga=T,f=100,...`). Detects `$TERM` or terminal response query; if supported, renders a 32x32 thumbnail in the detail panel.
   - **Fallback (Universal):** OSC 8 terminal hyperlink `\x1b]8;;{url}\x1b\\{text}\x1b]8;;\x1b\\` labeled `[LINK] Logo: {domain}`.

---

## 3. Proposed Changes & Implementation Steps

### 3.1. Schema & Protobuf Contract Changes

**`records/proto/src/main/proto/tracker/v1/company.proto`:**
Add fields 9 through 12:
```protobuf
message Company {
  string id = 1;
  string name = 2;
  repeated string aliases = 3;
  string website = 4;
  string industry = 5;
  google.protobuf.Timestamp createdAt = 6;
  google.protobuf.Timestamp updatedAt = 7;
  google.protobuf.Struct metadata = 8;
  string logoUrl = 9;                           // e.g. "https://logos.hunter.io/affirm.com"
  string logoFormat = 10;                       // e.g. "avif", "webp", "png"
  int32 logoSize = 11;                          // resolution in pixels (e.g. 256)
  google.protobuf.Timestamp logoFetchedAt = 12; // retrieval timestamp
}
```

**`tracker.schema.json`:**
Add properties under `$defs.company` (`additionalProperties: false` compliant):
```json
"logoUrl": {
  "$ref": "#/$defs/nullableString"
},
"logoFormat": {
  "$ref": "#/$defs/nullableString"
},
"logoSize": {
  "type": ["integer", "null"]
},
"logoFetchedAt": {
  "oneOf": [
    { "$ref": "#/$defs/isoDateTime" },
    { "type": "null" }
  ]
}
```

### 3.2. Kotlin Multiplatform Model & Dependencies

**`ui/kmp/gradle/libs.versions.toml`:**
Add Coil version catalog entries (aligned with [`add-coil-version-catalog.md`](../../../ui/kmp/.agents/plans/2026-11-08/173039_add-coil-version-catalog.md)):
```toml
[versions]
coil = "3.6.3"

[libraries]
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-core = { module = "io.coil-kt.coil3:coil-core", version.ref = "coil" }
coil-network-ktor3 = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }

[bundles]
coil = ["coil-compose", "coil-core", "coil-network-ktor3"]
```

**`ui/kmp/core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/Company.kt`:**
Add properties:
```kotlin
public val logoUrl: String? = null,
public val logoFormat: String? = null,
public val logoSize: Int? = null,
public val logoFetchedAt: Instant? = null,
```

**`ui/kmp/core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/factory/Factory.kt`:**
Update `createCompany()` signature and default arguments to accept logo fields.

**`ui/kmp/features/job-applications-list/ui/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/ui/component/JobApplicationDetailsScreen.kt`:**
Render the logo in `CompanyProfileSection`:
```kotlin
AsyncImage(
    model = company.logoUrl,
    contentDescription = "${company.name} Logo",
    modifier = Modifier.size(48.dp).clip(CircleShape),
)
```

### 3.3. Tracker CLI Subcommand & Usage Spec

**`scripts/specs/tracker.usage.kdl`:**
Register the subcommand to guarantee pre-commit and doc sync compliance:
```kdl
cmd "logo-fetch" help="Fetch and update company logos from Hunter.io" {
    flag "--dry-run" help="Preview logo resolution without writing to Firestore"
    flag "--company" help="Fetch logo for a single company ID" {
        arg "<id>"
    }
}
```

**`scripts/tracker.py`:**
Implement `commandLogoFetch(args)`:
1. Ensure write permission when not dry-run (`TRACKER_ALLOW_WRITES=1`).
2. Query unique companies from `applications` and existing `companies` docs.
3. Extract and normalize domain for each company.
4. Perform HTTP `HEAD` / `GET` check against `https://logos.hunter.io/{domain}`.
5. On HTTP 200, infer format from `Content-Type` header (`image/avif` -> `avif`), set `logoUrl`, and merge into `companies/{companyId}` document using `contract.toFirestore()`.
6. Log summary to stdout.

### 3.4. Terminal UI (`ui/terminal`)

**`scripts/tracker_tui.py`:**
- Support Kitty Graphics Protocol in Ghostty: emit terminal graphics escape sequences when Ghostty is detected.
- Universal fallback: Render `[LINK] Logo: {url}` formatted with OSC 8 clickable link sequences.

---

## 4. Verification Plan & Testing Checklist

### Automated Unit Tests
- Add unit tests in `scripts/tests/test_tracker_cli.py`:
  - `test_logo_fetch_dry_run`: Mocks `urllib.request` returning HTTP 200 and verifies no Firestore mutations occur.
  - `test_logo_fetch_404_skip`: Verifies that 404 responses do not overwrite or create invalid company records.
  - `test_domain_normalization`: Verifies extraction from complex URLs (`https://careers.google.com/jobs/` -> `google.com`).
- Run test suites inside sandbox:
  ```bash
  pytest scripts/tests/test_tracker_cli.py
  ```

### Protobuf & Schema Verification
- Compile Protobuf contracts:
  ```bash
  ./gradlew :records:proto:build
  ```
- Validate JSON schema:
  ```bash
  python3 -c "import json, jsonschema; schema = json.load(open('tracker.schema.json')); print('Schema valid')"
  ```

### Manual Verification
1. Run `mise run tracker logo-fetch --dry-run` to inspect domain derivation across existing applications.
2. Launch `mise run tui` in Ghostty to verify logo rendering in the application detail screen.
3. Run `mise run mise:docs:tasks` and `mise run completions:generate` to verify clean pre-commit passes.

---

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-08/171648_hunter-logo-integration.md`
- [x] Registry `README.md` row added / status synchronized
- [x] Implemented schema v9, proto, KMP model, and Compose UI AsyncImage integration
- [x] Implemented `mise run tracker logo-fetch` and populated 88/88 tracked companies in Firestore
- [x] Verified unit tests and documentation generation (`mise run mise:docs:tasks`)
