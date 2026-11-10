---
sessionId: session-261110-164342-8ugz
---

# Diagnosis & Fix

### Root Cause

When `mise run tracker:proposals list` executes, the wrapper script `.config/mise/tasks/tracker/proposals.py` calls `runpy.run_path()` to run `scripts/tracker.py`. This sets `sys.path[0]` to the wrapper's directory (`.config/mise/tasks/tracker/`).

When `tracker.py` then does `import proposals as proposalsModule`, Python resolves `proposals` to `.config/mise/tasks/tracker/proposals.py` — the **wrapper script itself** — because it's at `sys.path[0]`. The wrapper only defines `main()`, `Path`, `os`, `sys` — no `listing()` function.

The `sys.path.insert(0, SCRIPT_DIR)` in `tracker.py` (line 40-41) should place `scripts/` first, but `runpy.run_path()` with `run_name="__main__"` resets `sys.path[0]` to the wrapper's directory **before** `tracker.py` executes, and the `insert(0)` call places `scripts/` at position 0 — however the import still picks up the wrapper because of how `runpy` manipulates `sys.path`.

### Current State

- `.config/mise/tasks/tracker/proposals.py` — wrapper that delegates to `tracker.py` via `runpy.run_path()`
- `scripts/tracker.py` — main tracker, imports `proposals` at line 51
- `scripts/proposals.py` — actual proposals module with `listing()` at line 312
- `lib/proposals.py` — identical copy of `scripts/proposals.py`

### Proposed Fix

Rename the wrapper `.config/mise/tasks/tracker/proposals.py` to avoid the name collision. The wrapper should not share the same basename as a module that `tracker.py` imports. Rename it to `_proposals.py` or `proposals_cli.py` so Python's import system never finds it when resolving `import proposals`.

Alternatively, the fix can be in `tracker.py` to use an explicit import path, but renaming the wrapper is the cleanest fix — it addresses the root cause (name shadowing) and doesn't require changes to the core tracker logic.

### Files to Change

1. **Rename** `.config/mise/tasks/tracker/proposals.py` → `.config/mise/tasks/tracker/_proposals_cli.py`
2. **Update** any mise task references if needed (check `mise.toml` or task discovery)

# Delivery Steps

### ✓ Step 1: Fix wrapper to avoid module shadowing
Modified `.config/mise/tasks/tracker/proposals.py` to remove its own directory from `sys.path` before calling `runpy.run_path()`, and ensure `scripts/` is first in the path.

### ✓ Step 2: Verify fix and check for other shadowed wrappers
Confirmed `mise run tracker:proposals list` works correctly (returns 22 proposals). Checked all 21 sibling wrappers — only `proposals` had a name collision with a module imported by `tracker.py`.