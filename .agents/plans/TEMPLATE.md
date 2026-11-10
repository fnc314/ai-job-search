<!--
  Canonical plan template for every `.agents/plans/` directory in this repository
  (root, `records/.agents/plans/`, `frontend/.agents/plans/`).

  Usage:
    1. Copy to `<plans-dir>/YYYY-MM-DD/HHMMSS_<plan-name>.md` (24-hour clock).
    2. Fill the frontmatter block. `Status`, `Date`, `Target` are MANDATORY.
       Delete optional lines you don't need (Evaluates / Schema Authority / Depends On / Supersedes).
       When an AI model creates the plan, include `Harness` (the official name of the current running harness, e.g. Antigravity, Junie, Claude Code, OpenCode),
       `Model` (the model identifier currently running, e.g. gemini-3.8-flash, claude-3-7-sonnet), and `Session` (the current harness session/conversation ID).
    3. Each metadata line ends with two trailing spaces (Markdown hard line break).
    4. Register the plan in that directory's `README.md` table; its Status MUST match the registry row.
    5. No YAML `sessionId` blocks. Delete this comment.

  Status values: Draft / Proposed | In Review | In Progress | Completed | Superseded | Canonical
  Plan types:    Architecture Plan | Implementation Plan | Migration Plan
-->
# Implementation Plan: <Plan Title>

**Status:** Draft / Proposed  
**Date:** YYYY-MM-DD  
**Target:** <`records` | `frontend` | `backend` | `records` and `frontend` (cross-subsystem) | tracker | repository-wide>  
**Harness:** <current-running-harness-name>  
**Model:** <current-running-model-name>  
**Session:** <current-session-id>  
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