## `search:apply`

Manage job applications — evaluate fit, draft artifacts, and transition statuses via CLI


- **Usage:** `search:apply [FLAGS] [url_or_jd]`
- **Aliases:** `apply`

### Arguments
- **`[url_or_jd]`** — Job URL, file path, or job description text (optional; omit to list RANKED and DRAFTING applications)

### Flags
- **`--company <company>`** — Target company name override
- **`--role <role>`** — Target role title override
- **`--ai-rank`** — Run Tier 2 AI semantic calibration (default: enabled)
- **`--draft-assets`** — Generate tailored CV and cover letter sources
- **`--submitted`** — Mark application as SUBMITTED
- **`--persist`** — Persist status transitions to Firestore
- **`--output-dir <dir>`** — Root directory for generated artifacts