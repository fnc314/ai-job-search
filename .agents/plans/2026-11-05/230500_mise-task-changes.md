# Implementation Plan: Mise Task Changes

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** repository-wide (mise tasks, scripts)  
**Model:** Qwen/Qwen3.6-27B-FP8  
**Session:** session-261105-225654-kxq5

---

## 1. Executive Summary & Goals

Four changes to the `mise` task runner to improve the job search workflow:
1. Flip `mise scrape` output default from JSON to markdown, with `--json` as an opt-in.
2. Flip `mise scrape` to default to `--save-seen` (persist seen postings to Firestore), with `--no-save-seen` as an opt-out.
3. Add a top-level `mise apply` command that accepts a URL/JD or falls back to an interactive prompt over existing DRAFT applications.
4. Add a top-level `mise rank` command that accepts a required URL, local file path, or JD text and evaluates fit.

## 2. Current State

- **`mise scrape`** — Defined in `mise.toml`, backed by `scripts/scrape.py`. Previously defaulted to JSON output with `--table` for markdown; `--save-seen` was opt-in. Usage spec at `scripts/specs/scrape.usage.kdl`.
- **`mise apply`** — Did not exist. The `/apply` workflow lives as a Claude command (`.claude/commands/apply.md`) and is fully agent-driven.
- **`mise rank`** — Did not exist. The `/rank` workflow lives as a Claude command (`.claude/commands/rank.md`) and is agent-driven.
- **Tracker statuses** — `DRAFTING` (rank 1) represents applications with artifacts but not yet confirmed as submitted.

## 3. Proposed Changes & Implementation Steps

### 3.1. `scripts/scrape.py`
- Replace `--table` argument with `--json` (store_true). Default behavior becomes markdown table output.
- Replace `--save-seen` (store_true) with `--no-save-seen` (store_true). Default behavior becomes saving seen postings to Firestore.
- In `main()`: invert the conditional — Firestore DB is initialized by default; skipped only if `--no-save-seen`. Markdown table is the default output path; JSON is emitted only with `--json`.

### 3.2. `mise.toml` (scrape task)
- Replace `--table` flag with `--json` in the `usage` block.
- Replace `--save-seen` with `--no-save-seen`.
- Update the `run` template to pass the new flags.
- Update the description.

### 3.3. `scripts/specs/scrape.usage.kdl`
- Mirror the flag changes: `--json` replaces `--table`; `--no-save-seen` replaces `--save-seen`.

### 3.4. `scripts/apply.py` (new file)
- Accepts optional `[url_or_jd]` argument.
- **With argument:** queries `tracker list --json` to check for existing records; prints status and guidance.
- **Without argument (interactive):** queries `tracker list --status DRAFTING --json`, presents numbered list, allows user to view draft details, mark as submitted, or discard draft.

### 3.5. `scripts/rank.py` (new file)
- Accepts required `url_or_jd` argument (URL, local file path, or raw JD text) and optional `--json` flag.
- Detects input type: fetches posting content if URL, reads file content if local path, or uses text directly.
- Added `_read_file_content()` helper to read local file paths with error handling for missing files.
- Added path detection logic: if input contains `/`, `\`, or a file extension but the file doesn't exist, exit with an error.
- Loads profile and evaluation framework.
- Computes triage score (Technical 30%, Experience 25%, Behavioral 15%, Career 30%).
- Checks deal-breakers from candidate profile.
- Outputs markdown summary (default) or JSON (`--json`).

### 3.6. `mise.toml` (new tasks)
- Add `apply` task definition with usage block and run template.
- Add `rank` task definition with usage block and run template.

## 4. File Structure

```
Modified:
  mise.toml                          — scrape task flags + new apply/rank tasks
  scripts/scrape.py                  — inverted defaults for output and save-seen
  scripts/specs/scrape.usage.kdl     — mirrored flag changes

Added:
  scripts/apply.py                   — new CLI for apply workflow entry point
  scripts/rank.py                    — new CLI for single-job fit evaluation
```

## 5. Delivery Steps

### ✓ Step 1: Flip scrape defaults: markdown output and save-seen
Changed `mise scrape` to default to markdown table output and `--save-seen` behavior, added `--json` and `--no-save-seen` flags as opt-outs. Updated `scripts/scrape.py`, `mise.toml`, and `scripts/specs/scrape.usage.kdl`.

### ✓ Step 2: Add top-level `apply` mise task
Created `scripts/apply.py` — CLI entry point for the apply workflow. With a URL/JD argument, checks the tracker and prints guidance. Without arguments, enters interactive mode over DRAFTING-status applications. Added task definition to `mise.toml`.

### ✓ Step 3: Add top-level `rank` mise task
Created `scripts/rank.py` — lightweight single-job fit evaluator. Accepts required URL, local file path, or JD text, computes weighted triage score, checks deal-breakers, and outputs markdown or JSON. Added task definition to `mise.toml`.

### ✓ Step 4: Extend `rank` to accept local file paths
Expanded `scripts/rank.py` to detect and read local file paths. Added `_read_file_content()` helper and path detection logic in `main()` so that inputs containing `/`, `\`, or a file extension are treated as file paths (with error on missing files). Updated help text in `scripts/rank.py` and `mise.toml` to reflect the new input type.

## 6. Verification

- All three Python scripts compile without errors.
- `mise tasks ls` confirms `apply` and `rank` tasks are registered.
- `mise scrape` help shows `--json` and `--no-save-seen` flags with correct help text.
- Functional test of `mise rank` with sample JD produces correct output.
- `mise run rank patient-point-jd.txt` successfully reads and evaluates a local JD file (overall score 68/100, Moderate Fit).
- `mise run rank patient-point-jd.txt --json` returns structured JSON output when reading from a file.
- Non-existent file paths (e.g., `nonexistent-file.txt`, `docs/nonexistent/jd.txt`) correctly error with "File not found" (exit 1).
- Raw text input (e.g., `"Senior Android Engineer Kotlin"`) still works as before without path detection interference.

## 7. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-05/230500_mise-task-changes.md`
- [x] Registry `README.md` row added / status synchronized
