## `email:read-inbox`

Fetch job-related messages from email inbox (uses Composio integration to support Gmail or alternative providers) and write payload


- **Usage:** `email:read-inbox [FLAGS]`
- **Aliases:** `gmail:read:inbox`

### Flags
- **`-v --verbose`** — Print detailed progress and timing to stderr
- **`--label <label>`** — Fetch only this label/folder (e.g. Gmail label via Composio)
- **`--since <since>`** — Lookback start date, YYYY-MM-DD
- **`--max <max>`** — Total messages to collect, fetched 50 at a time
- **`--timeout <timeout>`** — Timeout in seconds per Composio API call (default: 60)
- **`--dry-run`** — Read email and compare labels, but write nothing