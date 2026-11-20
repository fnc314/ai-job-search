# Snapshot & Backup Lifecycle Management

This repository uses Firestore as its authoritative system of record. To preserve state fidelity, recover from regressions, and enable clean environment replication, database states are captured as immutable snapshots.

---

## 1. Snapshot Format & Authority

- **Format:** Newline-Delimited JSON (`.ndjson`). Each line represents a fully serialized Firestore document (applications, companies, events, counters) adhering strictly to [`tracker.schema.json`](./tracker.schema.json).
- **Canonical Storage Location:** `generated/snapshot-YYYY-MM-DD.ndjson` (local, gitignored).
- **Single Active Snapshot Policy:** Only the most recent validated state snapshot is preserved locally; older incremental dumps and legacy JSON formats (`backup_v7_*.json`) are deleted upon creation of a new snapshot.

---

## 2. Creating a Snapshot

Snapshots can be created using the `admin:backup` task runner:

```bash
# Export active Firestore state to generated/snapshot-<DATE>.ndjson
mise run admin:backup generated/snapshot-$(date +%Y-%m-%d).ndjson
```

Or directly via Gradle with the `TRACKER_ALLOW_WRITES=1` guardrail flag:

```bash
TRACKER_ALLOW_WRITES=1 ./gradlew -q :frontend:apps-cli:run --args="tracker export --ndjson" > generated/snapshot-$(date +%Y-%m-%d).ndjson
```

### Cleanup of Stale Snapshots
When a new snapshot is validated, purge older snapshots to keep the workspace clean:
```bash
rm -f generated/backup_*.json generated/backup_*.ndjson
```

---

## 3. Restoring from a Snapshot

Snapshots can be verified via dry-run and imported back into Firestore using `admin:restore`:

### Step 1: Dry-Run Validation (Safe)
Preview document counts and detect schema violations without performing database writes:
```bash
mise run admin:restore generated/snapshot-2026-11-20.ndjson --dry-run
```

### Step 2: Live Import
Apply the snapshot to Firestore (requires write permission):
```bash
TRACKER_ALLOW_WRITES=1 mise run admin:restore generated/snapshot-2026-11-20.ndjson
```

---

## 4. Automation & Pre-Migration Protocol

Whenever executing structural migrations (e.g. state progression changes, schema bumps, or pre-ingestion deduplication alignment):
1. **Push & Verify Local State:** Ensure all pending writes and state adjustments are flushed to Firestore.
2. **Snapshot Creation:** Run `mise run admin:backup generated/snapshot-$(date +%Y-%m-%d).ndjson`.
3. **Purge Obsolete Dumps:** Delete all previous snapshots.
4. **Log Registry Update:** Note the snapshot date in the relevant migration plan in `.agents/plans/`.
