# Qwen

Shared agent instructions are in `.agents/`:

- `.agents/README.md` — index and rules
- `.agents/skills/` — canonical skills (`job-tracker`, `job-scraper`, `upskill`, …)
- `AGENTS.md` — workspace role, candidate profile, shared rules
- `.claude/commands/` — the one implementation of the workflow commands

## Interface Execution Rule
Always use the `mise` interface (`mise run <task>` or `mise <task>`) for database mutations, tracker queries, TUI, and dashboard servers. Never invoke raw Python scripts or `.venv` binaries directly.
- Tracker CLI: `mise run tracker <command>` (e.g. `list`, `get`, `add`, `status`, `event`, `score`, `edit`)
- Data Alignment: `mise run align [id]`
- Terminal UI: `mise run tui`
- Dashboard: `mise run dashboard` / `mise run report`

Do not mirror these into `.qwen/`. Edit the source and leave this pointer.
