## `gmail:sync:labels`

Fetch and cache Gmail JOB labels (dry-run by default)


- **Usage:** `gmail:sync:labels [FLAGS]`

### Flags
- **`-v --verbose`** — Print detailed progress and timing to stderr
- **`--label <label>`** — Fetch only this label (validated against Gmail labels)
- **`--since <since>`** — Lookback start date, YYYY-MM-DD
- **`--timeout <timeout>`** — Timeout in seconds per Composio API call (default: 60)