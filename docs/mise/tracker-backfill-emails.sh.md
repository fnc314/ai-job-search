## `tracker:backfill:emails`

Correlate and backfill historical email IDs into JobApplication.emailMessageIds


- **Usage:** `tracker:backfill:emails [FLAGS]`

### Flags
- **`--apply`** — Write updates to Firestore (default: dry run)
- **`--state-file <path>`** — Path to gmail-sync/state.json
- **`--inbox-file <path>`** — Path to generated/gmail-inbox.json
- **`--job-labels-file <path>`** — Path to generated/gmail-job-labels.json