## `tracker:align`

Align applications in Firebase to canonical IDs, recalculate fit, and update outcome.md


- **Usage:** `tracker:align [FLAGS] [id]`
- **Aliases:** `align`

### Arguments
- **`[id]`** — Application ID or UUID to align

### Flags
- **`--app-id <app_id>`** — Application ID or UUID to align
- **`--all`** — Scan and align all applications
- **`--dry-run`** — Preview changes without writing
- **`--no-fit-recalc`** — Skip recalculating fit
- **`--no-docs-update`** — Skip updating outcome.md
- **`--note <note>`** — Optional audit note