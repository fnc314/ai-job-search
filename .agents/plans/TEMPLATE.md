<!--
  Canonical plan template for every `.agents/plans/` directory in this repository
  (root, `records/.agents/plans/`, `ui/kmp/.agents/plans/`).

  Usage:
    1. Copy to `<plans-dir>/YYYY-MM-DD/HHMMSS_<plan-name>.md` (24-hour clock).
    2. Fill the frontmatter block. `Status`, `Date`, `Target` are MANDATORY.
       Delete optional lines you don't need (Model / Session / Evaluates / Schema Authority / Depends On / Supersedes).
       When an AI model creates the plan, include `Model` (the model identifier) and `Session` (the session ID).
    3. Each metadata line ends with two trailing spaces (Markdown hard line break).
    4. Register the plan in that directory's `README.md` table; its Status MUST match the registry row.
    5. No YAML `sessionId` blocks. Delete this comment.

  Status values: Draft / Proposed | In Review | In Progress | Completed | Superseded | Canonical
  Plan types:    Architecture Plan | Implementation Plan | Migration Plan
-->
# Implementation Plan: <Plan Title>

**Status:** Draft / Proposed
**Date:** YYYY-MM-DD
**Target:** <`records` | `ui/kmp` | `records` and `ui/kmp` (cross-subsystem) | tracker | ui/terminal | repository-wide>
**Model:** <model-name>
**Session:** <session-id>
**Evaluates:** [`<draft>.md`](<relative-path>)
**Schema Authority:** [`tracker.schema.json`](<relative-path>)
**Depends On:** [`<plan>.md`](<relative-path>)
**Supersedes:** [`<plan>.md`](<relative-path>)

---

## 1. Executive Summary & Goals

## 2. Architecture & Design Principles

## 3. Proposed Changes & Implementation Steps

## 4. Verification Plan & Testing Checklist

## 5. Bookkeeping
- [ ] Plan persisted at `<plans-dir>/YYYY-MM-DD/HHMMSS_<plan-name>.md`
- [ ] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Stage changed files and craft a commit message based on the name of this file
- [ ] Push `git push origin main`