# Interface-First Naming: Exceptions & Violation Register

Policy: see "Interface-First Naming" in the root [`AGENTS.md`](../AGENTS.md). Public names describe the role
(`Remote`, `Local`, `Network`, `Blob`, `Email`, `Sheet`), never the tool. "Public" means **deployable**: anything that
does work (calls tools, reads/writes storage, produces artifacts) could be packaged to a Cloud Function/GCP.
Thin convenience wrappers are out of scope. `sheet` is a generic term and is allowed.

Enforcement: Konsist (`KonsistArchitectureTest`), Detekt `ForbiddenClassName`, and `mise run dev:guards`
(Kotlin + Python). All three honor [`interface-naming-baseline.txt`](./interface-naming-baseline.txt), which may only shrink.

## Documented exceptions
| Exception | Reason |
|---|---|
| `KmpInspector` (`frontend/apps/core`, `core/network`, `core/logging`) | Convenience tooling library, wired directly. |
| Direct `koin` API calls | Convenience DI library. |
| Code under an `impl/` package, or `internal`/`private` adapters | Adapters are where the tool is meant to live. |
| External-contract identifiers (Firestore collection/field names, env vars, third-party imports) | Cannot be renamed by us. |

Adding an exception requires a row here in the same change.

## Violation register (renames deferred to a follow-up plan)
| Symbol(s) | Where | Target interface name |
|---|---|---|
| `toFirestore()` / `fromFirestore()` | 11 model enums in `frontend/core/{data,artifacts}`; `records/parser` `TimestampConverter`, `JPypeBridge` | `toRemote()` / `fromRemote()` |
| `FirestoreProvider`, `firestore` | `frontend/apps/cli` | `RemoteDataSourceProvider`, `remote` |
| `firestoreInstance` | `frontend/apps/terminal` `TrackerApp` | `remoteInstance` |
| `createRoomDatabase` | `frontend/core/data` `DatabaseBuilder` | `createLocalDatabase` |
| `JPypeBridge` (file/class) | `records/parser` | `PythonBridge` |
| `toFirestore` / `fromFirestore` | `scripts/contract.py` | `to_remote` / `from_remote` |
| `gmail_fetch.py`, `composioBin` | `scripts/` | `email_fetch.py`, `emailClientBin` |
| `*_gcs_*` scripts and functions | `scripts/migrate_gcs.py`, `relocate_gcs_*`, `batch_relocate_gcs`, `purge_gcs_legacy` | `blob_*` |
| `*_firestore*` scripts and functions | `backup_firestore`, `restore_firestore`, `resume_migration`, `sync_local_artifacts` | `remote_*` |
| `gmail` naming in docs/skills | `.agents/commands/gmail-sync.md`, `.agents/skills/gmail*` | `email-sync` etc. |
