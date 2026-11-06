# Junie

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

Do not keep copies of skills or commands in `.junie/`. Read them from the paths
above, so a change lands for every agent at once.

## Plan Management Rule (MANDATORY)

All architecture, design, and migration plans **MUST** live in `.agents/plans/` following the invariants in [`.agents/plans/README.md`](../.agents/plans/README.md).

- **Never store plans in `.junie/`**. The `.junie/` directory is strictly for agent session configuration.
- Plans must use the `YYYY-MM-DD/HHMMSS_plan-name.md` filename hierarchy.
- Every plan must start from [`.agents/plans/TEMPLATE.md`](../.agents/plans/TEMPLATE.md).
- Register every plan in `.agents/plans/README.md` with status and scope.
- Subsystem-specific plans go in `<subsystem>/.agents/plans/` (e.g., `records/.agents/plans/`, `ui/kmp/.agents/plans/`).

This rule is non-negotiable. No exceptions.
