# Implementation Plan: Fix Invalid Usage Spec in `roadmap:edit` Mise Task

**Status:** Completed  
**Date:** 2026-11-10  
**Target:** `mise tasks (roadmap:edit)`  
**Harness:** Antigravity  
**Model:** Gemini 3.8 Flash (Low)  
**Session:** `67494617-dcfc-49c3-92fd-28e963ce3a2c`  

---

## 1. Executive Summary & Goal Description

### Problem
When `mise` parses task metadata or when running task-documentation tasks (such as `mise run docs:tasks`), it warns:
```text
mise WARN  invalid usage spec in task file ~/Work/Search/waleedtarbosh-ai-job-search/.config/mise/tasks/roadmap/edit.sh
  × Invalid usage config
   ╭─[/Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/.config/mise/tasks/roadmap/edit.sh:1:1]
 1 │ roadmap:edit — Launch the interactive roadmap TUI for managing ROADMAP.md items
   · ────────────┬
   ·             ╰── unsupported spec key roadmap:edit
   ╰────
```

### Root Cause
1. Line 2 of `.config/mise/tasks/roadmap/edit.sh` contains:
   `#USAGE roadmap:edit — Launch the interactive roadmap TUI for managing ROADMAP.md items`
   `mise` uses the [`usage`](https://usage.jdx.dev) CLI spec syntax for `#USAGE` directives, which only accepts keywords like `flag` and `arg` (e.g. `#USAGE flag ...` or `#USAGE arg ...`). `roadmap:edit` is not a valid spec key.
2. The task description is already declared by `#MISE description="Open the interactive roadmap manager (Kotlin TUI)"`. Because `roadmap:edit` takes no CLI flags or positional arguments, no `#USAGE` directive is required.
3. The script shebang (`#!/usr/bin/env bash`) was placed on line 4 after `#MISE` and `#USAGE` rather than on line 1.

---

## 2. Proposed Changes

### [Mise Tasks]
#### [MODIFY] `.config/mise/tasks/roadmap/edit.sh`
Align the task structure with canonical `.sh` tasks (like `.config/mise/tasks/ui/terminal/run.sh`):
- Move `#!/usr/bin/env bash` to line 1.
- Retain `#MISE description="Open the interactive roadmap manager (Kotlin TUI)"`.
- Delete the invalid `#USAGE` comment.
- Retain all execution logic (`set -euo pipefail`, binary check, Gradle installDist fallback, and execution of `apps-terminal --roadmap`).

---

## 3. Verification Plan

### Automated Verification
1. Run `mise run docs:tasks`:
   Verify that task documentation generation succeeds with **zero** `invalid usage spec` warnings.
2. Run `mise run dev:konsist`:
   Verify architectural compliance passes.
3. Smoke-test task discovery:
   Run `mise tasks ls | grep roadmap:edit` to verify that `roadmap:edit` lists properly with its description.

---

## 4. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-10/145600_fix-roadmap-edit-usage-spec-plan.md`
- [x] Registry `.agents/plans/README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Push `git push origin main`
