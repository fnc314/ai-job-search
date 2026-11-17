# Architecture & Migration Plan: Email Mise Tasks to Kotlin CLI Subsystem

**Status:** COMPLETED
**Date:** 2026-11-17
**Target:** Restore functionality of `email:sync-labels`, `email:read-inbox`, `email:collect`, and `email:discover` by delegating Mise tasks to Kotlin CLI (`:frontend:apps-cli`) and Composio mail integrations.

---

## 1. Problem Statement
In commit `d5161303`, Python task runners in `.config/mise/tasks/email/` were replaced with stub shell scripts printing `Completed.` without performing work.
The user requested that Mise email tasks invoke Kotlin JARs/CLI and perform the exact behaviors previously handled by the pipeline.

## 2. Proposed Architecture & Responsibilities

1. **Email Provider Operations (`email:sync-labels`, `email:read-inbox`)**:
   - `email:sync-labels`:
     - Invokes Composio CLI (or Gmail tool integration) to fetch email labels.
     - Updates `meta/sync` in Firestore with the current label hierarchy (root `JOB` and children).
     - Writes cached inventory to `generated/gmail-labels.json`.
   - `email:read-inbox`:
     - Uses query filter based on the cached `JOB` label hierarchy.
     - Fetches messages using Composio CLI and formats payload into `generated/gmail-inbox.json`.

2. **Ingestion & Proposal Staging (`email:collect`, `email:discover`) in Kotlin CLI (`:frontend:apps-cli`)**:
   - Add `EmailCommand` (and subcommands `collect`, `discover`, `sync-labels`, `read-inbox`) in `frontend/apps/cli/commands/EmailCommand.kt`.
   - `collect`:
     - Reads `generated/gmail-inbox.json` (or specified `--json` path).
     - Checks processed message IDs in Firestore `meta/sync`.
     - Uses `EmailClassifier` to classify status updates and transitions.
     - Stages changes into Firestore `pending-proposals`.
     - Updates `processedMessageIds` and `lastCollectedAt` in `meta/sync`.
   - `discover`:
     - Queries emails without associated `applicationId`.
     - Identifies companies not yet tracked using `EmailClassifier.extractCompany`.
     - Stages discovery proposals into `pending-proposals`.

3. **Mise Task Wrappers (`.config/mise/tasks/email/*.zsh`)**:
   - Each Mise task invokes `./gradlew -q :frontend:apps-cli:run --args="email <subcommand> $*"` passing through flags (`--dry-run`, `--since`, `--json`, etc.).

---
