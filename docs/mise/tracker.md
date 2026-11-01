## `tracker`

Run the tracker CLI (subcommands: check, seed, list, get, add, upsert, status, score, event, artifact, backfill, bulk, import, export, sheet, proposals, collect, labels, sync-due, mock-clear)


- **Usage:** `tracker [SUBCOMMAND]`

### `tracker check`

- **Usage:** `tracker check [--offline]`

Validate contract and connection

#### Flags
- **`--offline`** — Skip the Firestore reachability probe

### `tracker seed`

- **Usage:** `tracker seed`

Write meta/schema from the contract

### `tracker list`

- **Usage:** `tracker list [FLAGS]`

List applications

#### Flags
- **`--status`** — Filter by status
- **`--open`** — Only open applications
- **`--tag`** — Filter by an exact tag value
- **`--limit`** — Max results to return
- **`--json`** — Output as JSON
- **`--tui`** — Launch interactive TUI with these filters

### `tracker get`

- **Usage:** `tracker get <id>`

Show one application and its events

#### Arguments
- **`<id>`** — Application ID

### `tracker add`

- **Usage:** `tracker add [--json] [--dry-run]`

Create an application from a JSON payload

#### Flags
- **`--json`** — Payload file, or - for stdin
- **`--dry-run`** — Resolve and validate only

### `tracker upsert`

- **Usage:** `tracker upsert [--json] [--dry-run]`

Create-or-merge from a JSON payload

#### Flags
- **`--json`** — Payload file, or - for stdin
- **`--dry-run`** — Resolve and validate only

### `tracker status`

- **Usage:** `tracker status [FLAGS] <id> <status>`

Record a funnel status change

#### Arguments
- **`<id>`** — Application ID
- **`<status>`** — Target status

#### Flags
- **`--date`** — ISO date the change happened
- **`--note`** — What happened
- **`--correction`** — Emit status-corrected event
- **`--dry-run`** — Preview without writing

### `tracker score`

- **Usage:** `tracker score [FLAGS] <id>`

Record a recalculated fit rating

#### Arguments
- **`<id>`** — Application ID

#### Flags
- **`--fit`** — Integer 0-100
- **`--date`** — ISO date the score was decided
- **`--note`** — What changed in the evaluation
- **`--dry-run`** — Preview without writing

### `tracker event`

- **Usage:** `tracker event [FLAGS] <id>`

Append a timeline event

#### Arguments
- **`<id>`** — Application ID

#### Flags
- **`--type`** — Event type (e.g. event:interview)
- **`--date`** — ISO date the event happened
- **`--note`** — Event note
- **`--json`** — Extra fields as JSON
- **`--dry-run`** — Preview without writing

### `tracker artifact`

- **Usage:** `tracker artifact [FLAGS] <id>`

Register and upload a file

#### Arguments
- **`<id>`** — Application ID

#### Flags
- **`--kind`** — Artifact type (e.g. artifact:cv)
- **`--path`** — Local file path
- **`--filename`** — Override stored filename
- **`--no-upload`** — Record the path only
- **`--dry-run`** — Preview without writing

### `tracker backfill`

- **Usage:** `tracker backfill [FLAGS]`

Add hashes and storage to registered artifacts

#### Flags
- **`--apply`** — Write the updates (default: report only)
- **`--root`** — Repo root for relative paths
- **`--limit`** — Stop after N files

### `tracker bulk`

- **Usage:** `tracker bulk [FLAGS]`

Add many applications from one JSON file

#### Flags
- **`--json`** — JSON array or {applications: [...]}
- **`--allow-partial`** — Write the valid rows
- **`--dry-run`** — Preview without writing

### `tracker import`

- **Usage:** `tracker import [FLAGS]`

One-shot import of the legacy CSV

#### Flags
- **`--csv`** — Path to legacy tracker CSV
- **`--dry-run`** — Report only, never write
- **`--mock`** — Skip historical profile reconciliation
- **`--report`** — Write the reconciliation report

### `tracker export`

- **Usage:** `tracker export [FLAGS]`

Read the record back out

#### Flags
- **`--json`** — Full JSON payload
- **`--csv`** — Legacy CSV shape
- **`--ndjson`** — One application per line
- **`-o --output`** — Write to a file instead of stdout
- **`--refresh-cache`** — Also refresh tracker-cache.json

### `tracker sheet`

- **Usage:** `tracker sheet [FLAGS]`

Regenerate the Sheets view

#### Flags
- **`--sync`** — Accepted for symmetry
- **`--sheet-id`** — Spreadsheet ID
- **`--local-only`** — Write CSVs locally
- **`--directory`** — Where --local-only writes

### `tracker proposals`

- **Usage:** `tracker proposals [SUBCOMMAND]`

Review the collector queue

### `tracker proposals list`

- **Usage:** `tracker proposals list [--state]`

List proposals

#### Flags
- **`--state`** — Filter by state (e.g. proposal:pending)

### `tracker proposals show`

- **Usage:** `tracker proposals show <id>`

Show a proposal

#### Arguments
- **`<id>`** — Proposal ID

### `tracker proposals approve`

- **Usage:** `tracker proposals approve [--note] [--only] <id>`

Approve a proposal

#### Arguments
- **`<id>`** — Proposal ID

#### Flags
- **`--note`** — Resolution text
- **`--only`** — Comma-separated change indexes to approve

### `tracker proposals reject`

- **Usage:** `tracker proposals reject [--reason] <id>`

Reject a proposal

#### Arguments
- **`<id>`** — Proposal ID

#### Flags
- **`--reason`** — Why it was rejected

### `tracker proposals expire`

- **Usage:** `tracker proposals expire`

Expire stale proposals

### `tracker collect`

- **Usage:** `tracker collect [FLAGS]`

Stage proposals from fetched messages

#### Flags
- **`--json`** — Inbox payload path
- **`--labels`** — JSON inventory of Gmail labels
- **`--trigger`** — Trigger source: manual | cron | application-updated
- **`--dry-run`** — Preview without writing

### `tracker labels`

- **Usage:** `tracker labels`

Show Gmail labels seen at the last sync

### `tracker sync-due`

- **Usage:** `tracker sync-due [--json] [--dry-run]`

Run due sync-requests

#### Flags
- **`--json`** — Inbox payload for every request
- **`--dry-run`** — List what is due

### `tracker mock-clear`

- **Usage:** `tracker mock-clear [--apply]`

Delete namespaced mock records

#### Flags
- **`--apply`** — Actually delete (default: dry run)