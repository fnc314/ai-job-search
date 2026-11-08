---
name: job-tracker
description: >
  Operates the job application record. Firestore is the only write target and
  `mise run tracker` (or `mise tracker`) is the primary way to interact with it;
  the Gmail collector stages proposals for review, artifacts live in GCS, and the
  Google Sheet is a regenerated view. Use for tracking applications, recording outcomes,
  recalculating fit, syncing Gmail status, regenerating the Sheet or dashboard,
  or answering "what does the tracker know?".
---

# Job Tracker

One record, one write path, one history. This skill states the invariants an
agent must not break; the detail lives in `.agents/plans/2026-10-31/021052_tracker-migration-plan.md` and
the contract in `tracker.schema.json`.

## The three rules

1. **Firestore is the record.** `job_search_tracker.csv` is a frozen pre-migration
   snapshot in `archive/`. Never read it as the source and never write it.
2. **Every write goes through `mise run tracker` or `mise run align`.** No hand-edited documents,
   no direct Firestore writes from a workflow, no raw Python script executions, no CSV cells. Writes are opt-in:
   the CLI refuses unless `TRACKER_ALLOW_WRITES=1`.
3. **History is append-only.** A status change is a new event. A correction is
   `event:status-corrected`, not an edit. Derived fields (`isOpen`, counters,
   `notesRendered`, `timeline`) are projections of the log and are recomputed,
   never typed.

## Commands

| goal | command |
|---|---|
| Inspect | `mise run tracker list [--open --status --tag --limit]`, `mise run tracker get <id>` |
| Add | `mise run tracker add --json <file\|->`, `upsert --json …`, `bulk --json …` |
| Move the funnel | `mise run tracker status <id> <status> [--date --note] [--correction]` |
| Record activity | `mise run tracker event <id> --type event:* [--date --note --json -]` |
| Record a fit change | `mise run tracker score <id> --fit N [--note]` |
| Align & Migrate | `mise run align [id]` or `mise run tracker align [id]` |
| Register a file | `mise run tracker artifact <id> --kind artifact:* --path P` |
| Review Gmail | `mise run tracker proposals list\|show\|approve\|reject` |
| Fetch support | `mise run tracker labels` (last seen Gmail labels) |
| Views | `mise run tracker sheet --sync`; `mise run report`; `mise run dashboard` |
| Verify | `mise run tracker check`; `mise run tracker export --csv` is the rollback path |

All commands print JSON on stdout. Status accepts legacy labels (`applied`) or
namespaced values (`status:applied`); the CLI normalizes. Never invent a status
or an event type — both are closed enums in `tracker.schema.json`.

## Reading

Read the record using `mise run tracker list` or `mise run tui` (the TUI and dashboard read
from the store or local cache). It returns the record with a provenance badge: `store (live)`, `cache (…old)`,
`csv (pre-migration)`. `TRACKER_SOURCE=store|cache|csv` forces a source.
A UI that shows stale data must say so; never hide the badge.

## Email Synchronization

Email ingestion operates via Composio integration, supporting Gmail or alternative email providers:

- **Fetch + stage:** read email via `mise run email:read-inbox`, write `generated/gmail-inbox.json` and
  `generated/gmail-labels.json`, run `mise run email:collect --labels …`.
  `collect` stages one proposal and writes `meta/sync` only. It never touches
  applications.
- **Review:** present `mise run proposals show <id>`, wait for an explicit reply, then
  `mise run proposals approve <id> [--only 1,3]`. Approval emits events in one batch.

Scopes: Primary, Updates, `JOBS`, and every `JOBS/*` label/folder seen last run.
`--label` is validated against `mise run tracker labels`; unknown values are refused,
not guessed.

## Artifacts and views

- Blob metadata (`sha256`, `size`, `contentType`, `storage`) lives on the
  artifact document; bytes live in `gs://job-search-fnc314-dev-artifacts`.
  `.gdoc` shortcuts and `externalUrl` records have no blob by design.
- The Google Sheet is regenerated wholesale (`mise run tracker sheet --sync`) and is never
  hand-edited.
- `generated/` holds disposable caches. Local documents stay in `documents/`.

## Automating

Two `launchd` user agents, both reading credentials already on disk (the tracker
service account and the Composio CLI login):

- `dev.fnc314.tracker-gmail-fetch` — every 12 hours, runs
  `mise run email:read-inbox`: writes `generated/gmail-inbox.json`
  and `gmail-labels.json`, runs `mise run email:collect --trigger trigger:cron` to stage a
  proposal, then `mise run tracker sync-due`. Review stays interactive.
- `dev.fnc314.tracker-collect` — every 15 minutes, runs `mise run tracker sync-due`.
  A write queues one deferred collection run; `sync-due` leaves requests pending
  when no inbox payload exists, so a missing fetch never consumes the backlog.

The fetcher pages 50 messages at a time because Composio rejects larger single
responses, and `collect` records every message it examined — not only those that
produced a change — so a scheduled run over the same mail stages nothing twice.

Inspect with `launchctl print gui/$(id -u)/<label>` and
`~/Library/Logs/job-tracker-*.log`; install from `deploy/launchd/`.

## Self-Documenting Mise Tasks

All commands are defined as modular, executable files in `.config/mise/tasks/`:

1. **Add new subcommands or tasks** by creating a new `.py` file in `.config/mise/tasks/<subsystem>/<name>.py`.
2. **Add metadata and CLI specs** using `#MISE description="..."` and `#USAGE flag ...` or `#USAGE arg ...` comment headers directly in the file.
3. **Regenerate docs & completions** via `mise run docs:tasks` and `mise run completions:generate`.

## Before finishing a tracker task

- `TRACKER_ALLOW_WRITES=1` was set deliberately for the write, not exported
  globally by accident.
- The change is an event, and `mise run tracker get <id>` shows it.
- If the Sheet matters, regenerate it; do not describe it as current otherwise.
- Never commit personal data (`documents/`, `generated/`, `.env.local`).
- If you added a new CLI subcommand, verify `mise tracker <subcommand> --help`
  works and that `scripts/specs/tracker.usage.kdl` includes it.
