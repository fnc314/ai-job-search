## `tracker:emails:backfill`

Ingest and correlate emails from generated/gmail-inbox.json into Firestore emails collection


- **Usage:** `tracker:emails:backfill [--dry-run] [--local]`

### Flags
- **`--dry-run`** — Dry run without writing to Firestore
- **`--local`** — Execute directly via local backend runner rather than Cloud Run