# Architecture & Feature Plan: Proposal Edit CLI Command (`proposals edit`)

**Status:** COMPLETED
**Date:** 2026-11-17
**Target:** Add `proposals edit` subcommand to `:frontend:apps-cli` allowing granular modification, deletion, and status reclassification of proposed changes before approval.

---

## 1. Problem & Context
When email messages or automated sync proposals are staged in `pending-proposals`, the automated classifier may occasionally misinterpret an email (e.g., classifying a rejection email as `RECEIVED`, or interpreting a job-board digest as a new employer `LEAD`). Users need a CLI capability to modify, reclassify, or remove proposed changes from a pending proposal before executing approval (`proposals approve`).

## 2. CLI Interface Design (`proposals edit`)
```
Usage: jobsearch proposals edit <id> [options]

Arguments:
  id                      Proposal ID to edit

Options:
  --item, -i <index>      1-based index of proposed change to modify or remove (required)
  --event <type>          Change eventType (e.g. REJECT, SCREEN, OFFER, INTERVIEW, RECEIVED, LEAD)
  --status-to <status>    Target status transition (e.g. REJECTED, INTERVIEWING, OFFER, CONSIDERING, NONE)
  --app-id <appId>        Assign or override target application ID
  --remove                Remove the selected change from proposed changes (moves to unmatched if email exists)
  --note <note>           Optional note/reason explaining the edit
```

## 3. Implementation Steps
1. Add `EditProposalCommand` in `frontend/apps/cli/src/main/kotlin/dev/fnc314/aijobtracker/frontend/cli/commands/ProposalCommand.kt`.
2. Register `EditProposalCommand` in `ProposalCommand`'s subcommands.
3. Verify with proposal `6043f260-6798-4451-a192-e5aeb7be9be6`:
   - Edit change [1] (AARP): set `--event REJECT --status-to REJECTED`.
   - Remove change [2] (Glassdoor marketing digest): `--item 2 --remove`.
   - Inspect proposal with `mise run tracker proposals show 6043f260-6798-4451-a192-e5aeb7be9be6`.
