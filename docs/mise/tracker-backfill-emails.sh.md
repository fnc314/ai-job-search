## `tracker:backfill:emails`

Correlate and backfill historical email IDs into JobApplication.emailMessageIds


- **Usage:** `tracker:backfill:emails [--apply] [--state-file <path>]`

### Flags
- **`--apply`** — Write updates to Firestore (default: dry run)
- **`--state-file <path>`** — Path to gmail-sync/state.json