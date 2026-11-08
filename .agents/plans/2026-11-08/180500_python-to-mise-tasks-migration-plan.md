# Migration Plan: Migrate Direct Python Invocations to File-Based Mise Tasks

**Status:** Completed  
**Date:** 2026-11-08  
**Target:** repository-wide  
**Model:** Gemini 3.8 Flash  
**Session:** a429b3a2-3455-4c8e-9b1b-eccae6ec2e1f  
**Depends On:** [`.agents/plans/2026-11-05/230500_mise-task-changes.md`](./2026-11-05/230500_mise-task-changes.md)  

---

## 1. Executive Summary & Goals

### Problem Statement
Currently, workflows, documentation, and skill definitions across this repository reference or directly invoke Python binaries (`.venv/bin/python`, `python3`, or `python`). In `mise.toml`, Python tasks are declared inline with long shell command strings, including a 160-line Jinja-templated string for the `tracker` task. Furthermore, CLI specifications are fragmented across `scripts/specs/*.usage.kdl`, `mise.toml` `usage = """..."""`, and Python `argparse` blocks.

This workspace is a multi-language monorepo containing `records` (Protobuf contracts & JVM parser), `ui/kmp` (Kotlin Multiplatform Compose Desktop, Android, and Web), `ui/terminal` (Curses TUI), `ui/web` (Web server & preview), `tracker` data stores, Bun/TS portal search skills, and future specialized CLI tools.

### Core Objectives
1. **Obscure Python Execution**: Treat Python as an internal execution engine rather than a CLI invocation surface. The CLI boundary for users, AI agents, skills, and CI is strictly `mise <task>` / `mise run <task>`.
2. **Self-Contained & Self-Documenting Tasks**: Migrate all task configurations and CLI `usage` templates directly into the task files (`.config/mise/tasks/**/*.py`). Each file contains all `#MISE` metadata, `#USAGE` flags/args, and execution code needed to run, document, and generate completions.
3. **Retire External Spec Files**: Completely retire separate `scripts/specs/*.usage.kdl` and inline Jinja templates in `mise.toml`.
4. **Monorepo Subsystem Namespacing**: Organize tasks under modular, subsystem-first namespaces in `.config/mise/tasks/` (`records:*`, `ui:kmp:*`, `ui:terminal:*`, `ui:web:*`, `tracker:*`, `email:*`, `search:*`, `salary:*`, `dev:*`, `docs:*`).
5. **Monorepo Config Roots**: Align `mise.toml` `[monorepo] config_roots` by adding `records` alongside `ui/kmp` and toolchains.
6. **Transparent Virtualenv & Environment Binding**: Utilize Mise's native `_.python.venv` configuration to automatically bind the pinned Python interpreter (`3.15.2`) and `.venv` dependencies, eliminating hardcoded `.venv/bin/python` paths.
7. **Hard-Block Direct Invocations**: Implement a strict runtime block (`lib/_guard.py`) that exits immediately with error status 1 if any script is executed directly outside of Mise.
8. **Archive Legacy Scripts**: Relocate one-off and obsolete scripts from `scripts/` to `archive/scripts/`, isolating shared data libraries under `lib/tracker/` and completely emptying `scripts/` of loose runnable entrypoints.
9. **Strict Alignment Across All Documentation & Skills**: Update `README.md`, `AGENTS.md`, `GEMINI.md`, `SETUP.md`, `CONTRIBUTING.md`, `.claude/commands/*.md`, and **ALL `SKILL.md` files** to strictly use `mise run <task>` or `mise <task>` for all operations.

---

## 2. Monorepo Architecture & Self-Documenting Tasks

### 2.1 The Self-Contained Task File Paradigm
Every task file under `.config/mise/tasks/` encapsulates:
- **Metadata**: `#MISE description="..."`, `#MISE alias="..."`, `#MISE depends=[...]`.
- **CLI Specification**: `#USAGE flag ...` and `#USAGE arg ...` annotations embedded directly in comments at the top of the file.
- **Execution & Parameter Binding**: Mise parses `#USAGE` at runtime, exports typed environment variables (`usage_<flag>`, `usage_<arg>`), handles `--help` natively, and forwards raw arguments to the task.
- **Self-Documentation**: `mise generate task-docs` reads the embedded `#USAGE` and `#MISE` comments to auto-generate markdown documentation in `docs/mise/`.
- **Completion Generation**: Completions are generated directly from the tasks' embedded Usage specs, completely eliminating `scripts/specs/`.

### 2.2 Subsystem Map & Namespacing
| Subsystem | Scope / Purpose | Primary Task Namespace | Aliases / Shortcuts |
| :--- | :--- | :--- | :--- |
| **`records`** | Protobuf contracts & JVM parsing engine | `records:build`, `records:test`, `records:docs` | `records:docs:generate` |
| **`ui/kmp`** | Kotlin Multiplatform client suite | `ui:kmp:build`, `ui:kmp:desktop:run`, `ui:kmp:android:assemble`, `ui:kmp:web:wasm:dev` | |
| **`ui/terminal`**| Interactive Curses TUI | `ui:terminal:run` | `tui` |
| **`ui/web`** | Web dashboard server & HTML preview | `ui:web:server`, `ui:web:report` | `dashboard`, `report` |
| **`tracker`** | Firestore store & application lifecycle | `tracker:check`, `tracker:list`, `tracker:add`, `tracker:align`, `tracker:default` | `tracker <cmd>`, `align` |
| **`email`** | Email label sync, inbox fetch, proposal staging | `email:sync-labels`, `email:read-inbox`, `email:collect` | `gmail:*` (transition) |
| **`search`** | Job portal search aggregators & fit ranker | `search:scrape`, `search:rank`, `search:apply` | `scrape`, `rank`, `apply` |
| **`salary`** | Salary benchmark lookup & Excel convert | `salary:lookup`, `salary:convert-excel` | `salary` |
| **`dev`** | Monorepo tests, linters, security guards | `dev:test`, `dev:lint`, `dev:detekt`, `dev:konsist`, `dev:guards` | `test`, `lint`, `detekt`, `konsist`, `security-guards` |
| **`docs`** | Monorepo documentation orchestrators | `docs:tasks`, `docs:all` | `mise:docs:tasks`, `docs:all` |

### 2.3 Shared Environment & Virtualenv State
Mise serves as the unified environment orchestrator:
```toml
[monorepo]
  lockfile = true
  config_roots = [ "records", "ui/kmp", "ui/kotlin-toolchain" ]

[tools]
  python = "3.15.2"

[env]
  _ = { file = ".env.local" }
  _.python.venv = { path = ".venv", create = false }
  PYTHONPATH = "{{config_root}}/lib:{{config_root}}"
  TRACKER_ALLOW_WRITES = "1"
  TRACKER_SOURCE = "store"
```

---

## 3. Directory Taxonomy & Work Breakdown

```
.config/mise/tasks/
├── records/
│   ├── build.sh            # records:build (cd records && ./gradlew build)
│   ├── test.sh             # records:test (cd records && ./gradlew test)
│   ├── detekt.sh           # records:detekt (cd records && ./gradlew detekt)
│   └── docs.sh             # records:docs (alias="records:docs:generate")
├── ui/
│   ├── kmp/
│   │   ├── build.sh        # ui:kmp:build
│   │   ├── clean.sh        # ui:kmp:clean
│   │   ├── test.sh         # ui:kmp:test
│   │   ├── desktop/
│   │   │   ├── run.sh      # ui:kmp:desktop:run
│   │   │   └── package.sh  # ui:kmp:desktop:package
│   │   ├── android/
│   │   │   ├── assemble.sh # ui:kmp:android:assemble
│   │   │   ├── bundle.sh   # ui:kmp:android:bundle
│   │   │   └── install.sh  # ui:kmp:android:install
│   │   ├── web/
│   │   │   ├── wasm/
│   │   │   │   ├── dev.sh  # ui:kmp:web:wasm:dev
│   │   │   │   └── dist.sh # ui:kmp:web:wasm:dist
│   │   │   └── js/
│   │   │       ├── dev.sh  # ui:kmp:web:js:dev
│   │   │       └── dist.sh # ui:kmp:web:js:dist
│   │   ├── detekt.sh       # ui:kmp:detekt
│   │   ├── docs.sh         # ui:kmp:docs (alias="ui:kmp:docs:generate")
│   │   └── readmes.sh      # ui:kmp:readmes (alias="ui:kmp:docs:readmes")
│   ├── terminal/
│   │   └── run.py          # ui:terminal:run (alias="tui")
│   └── web/
│       ├── server.py       # ui:web:server (alias="dashboard")
│       └── report.py       # ui:web:report (alias="report")
├── tracker/
│   ├── default.py          # tracker:default (alias="tracker", unified dispatcher)
│   ├── check.py            # tracker:check (with embedded #USAGE)
│   ├── seed.py             # tracker:seed (with embedded #USAGE)
│   ├── list.py             # tracker:list (with embedded #USAGE)
│   ├── get.py              # tracker:get (with embedded #USAGE)
│   ├── add.py              # tracker:add (with embedded #USAGE)
│   ├── upsert.py           # tracker:upsert (with embedded #USAGE)
│   ├── edit.py             # tracker:edit (with embedded #USAGE)
│   ├── status.py           # tracker:status (with embedded #USAGE)
│   ├── score.py            # tracker:score (with embedded #USAGE)
│   ├── lead.py             # tracker:lead (with embedded #USAGE)
│   ├── align.py            # tracker:align (alias="align", with embedded #USAGE)
│   ├── bulk.py             # tracker:bulk (with embedded #USAGE)
│   ├── import.py           # tracker:import (with embedded #USAGE)
│   ├── export.py           # tracker:export (with embedded #USAGE)
│   ├── sheet.py            # tracker:sheet (with embedded #USAGE)
│   ├── mock-clear.py       # tracker:mock-clear (with embedded #USAGE)
│   ├── sync-due.py         # tracker:sync-due (with embedded #USAGE)
│   └── labels.py           # tracker:labels (with embedded #USAGE)
├── proposals/
│   ├── list.py             # proposals:list (with embedded #USAGE)
│   ├── show.py             # proposals:show (with embedded #USAGE)
│   ├── approve.py          # proposals:approve (with embedded #USAGE)
│   ├── reject.py           # proposals:reject (with embedded #USAGE)
│   └── expire.py           # proposals:expire (with embedded #USAGE)
├── email/
│   ├── sync-labels.py      # email:sync-labels (alias="gmail:sync:labels", with embedded #USAGE)
│   ├── read-inbox.py       # email:read-inbox (alias="gmail:read:inbox", with embedded #USAGE)
│   └── collect.py          # email:collect (alias="gmail:collect", with embedded #USAGE)
├── artifacts/
│   ├── add.py              # artifacts:add (with embedded #USAGE)
│   ├── backfill.py         # artifacts:backfill (with embedded #USAGE)
│   ├── migrate.py          # artifacts:migrate (alias="migrate-gcs", with embedded #USAGE)
│   └── purge-legacy.py     # artifacts:purge-legacy (alias="purge-gcs-legacy", with embedded #USAGE)
├── search/
│   ├── scrape.py           # search:scrape (alias="scrape", with embedded #USAGE)
│   ├── migrate.py          # search:migrate (alias="scrape:migrate", with embedded #USAGE)
│   ├── rank.py             # search:rank (alias="rank", with embedded #USAGE)
│   └── apply.py            # search:apply (alias="apply", with embedded #USAGE)
├── salary/
│   ├── lookup.py           # salary:lookup (alias="salary", with embedded #USAGE)
│   └── convert-excel.py    # salary:convert-excel (with embedded #USAGE)
├── dev/
│   ├── test.py             # dev:test (alias="test")
│   ├── lint.py             # dev:lint (alias="lint", runs Detekt + Konsist)
│   ├── detekt.py           # dev:detekt (alias="detekt")
│   ├── konsist.py          # dev:konsist (alias="konsist")
│   └── guards.py           # dev:guards (alias="security-guards")
├── docs/
│   ├── tasks.py            # docs:tasks (alias="mise:docs:tasks")
│   └── all.py              # docs:all
└── completions/
    └── generate.py         # completions:generate
```

---

## 4. Implementation Steps

### Phase 1: Environment & Virtualenv Setup
1. Update `mise.toml`:
   - Add `"records"` to `[monorepo] config_roots`.
   - Add `_.python.venv = { path = ".venv", create = false }` to `[env]`.
   - Add `PYTHONPATH = "{{config_root}}/lib:{{config_root}}"` to `[env]`.
   - Strip all inline Python task blocks from `mise.toml`.

### Phase 2: Create Self-Contained, Self-Documenting File Tasks
1. Create directory structure `.config/mise/tasks/`.
2. Port subsystem tasks for `records/`, `ui/kmp/`, `ui/terminal/`, `ui/web/`, `search/`, `salary/`, `email/`, `artifacts/`, `docs/`, `dev/`, and `admin/`.
3. Port each `tracker` command into its own task file with embedded `#USAGE` annotations:
   - Provide `tracker/default.py` with `#MISE alias="tracker"` to maintain `mise tracker <subcommand>` compatibility.
   - Decompose into granular subtasks (`tracker/check.py`, `tracker/list.py`, `tracker/align.py`, etc.) for direct invocation (`mise tracker:check`).
4. Retire `scripts/specs/*.usage.kdl` to `archive/specs/`.

### Phase 3: Archive Legacy Scripts & Hard Block Enforcement
1. **Relocate Legacy Scripts**:
   - Move one-off migration and fix scripts (`migrate_v8.py`, `fix_*.py`, `parse_html*.py`, `cross_reference.py`, `organize_docs.py`) to `archive/scripts/`.
   - Move shared data logic modules (`contract.py`, `client.py`, `events.py`, etc.) to `lib/tracker/`.
2. **Hard Block Direct Execution**:
   Add hard block enforcement in `lib/_guard.py`:
   ```python
   import os, sys

   def enforce_mise():
       if not os.environ.get("MISE_TASK") and not os.environ.get("ALLOW_DIRECT_PYTHON"):
           sys.stderr.write(
               "\n[ERROR] Direct Python execution is strictly disallowed in this repository.\n"
               "All operations must be invoked via Mise:\n"
               "  -> Use 'mise run <task>' or 'mise <task>'\n"
               "  -> Run 'mise tasks ls' to view available tasks.\n\n"
           )
           sys.exit(1)
   ```
3. **Security Guards & Settings**:
   - Update `scripts/security_guards.py` and `.claude/settings.json` to allowlist `Bash(mise run:*)` and `Bash(mise:*)` instead of direct Python commands.
   - Update `.git/hooks/pre-commit` to prevent committing raw `python scripts/...` invocations.

### Phase 4: Align All Documentation, Commands, and SKILL.md Files
1. Update `README.md`, `AGENTS.md`, `GEMINI.md`, `CODEX.md`, `OPENCODE.md`, `CONTRIBUTING.md`, and `SETUP.md`.
2. Update `.claude/commands/*.md`.
3. Update **ALL `SKILL.md` files** (`.agents/skills/**/SKILL.md`, `.claude/skills/**/SKILL.md`) to ensure any tool or agent skill invokes commands via `mise run <task>`.

---

## 5. Verification & Testing Checklist

- [x] `mise tasks ls --all` displays all monorepo file tasks across `records:*`, `ui:*`, `tracker:*`, `email:*`, etc.
- [x] `mise tracker check` and `mise tracker:check` succeed without warnings.
- [x] `mise tracker:check --help` renders formatted help parsed directly from embedded `#USAGE` annotations.
- [x] `mise email:sync-labels` runs dry-run label sync.
- [x] `mise align --dry-run` functions identically to existing script.
- [x] `mise tui` opens the Curses TUI without hardcoding `.venv/bin/python`.
- [x] `mise run test` discovers and runs unit tests under the managed virtualenv.
- [x] `mise:docs:tasks` generates accurate documentation in `docs/mise/` directly from file tasks.
- [x] Attempting to run `python3 scripts/tracker.py` triggers the hard enforcement guard (exit 1).
- [x] `git diff` confirms all `SKILL.md` files use `mise run <task>` exclusively.
- [x] Pre-commit hook passes and CI succeeds.

---

## 6. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-08/180500_python-to-mise-tasks-migration-plan.md`
- [x] Registry `README.md` row added / status synchronized
- [x] User review and approval received
