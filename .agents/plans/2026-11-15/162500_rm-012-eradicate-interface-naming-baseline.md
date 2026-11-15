# Implementation Plan: Eradicate Interface Naming Baseline Violations (RM-012)

**Status:** Completed  
**Date:** 2026-11-15  
**Target:** repository-wide  
**Depends On:** [`022100_interface-first-naming-policy.md`](./2026-11-14/022100_interface-first-naming-policy.md)  

---

## 1. Executive Summary & Goals

This plan executed **[RM-012] ("Eradicate Interface Naming Baseline Violations")**, systematically renaming all 51 legacy tool-named symbols and file paths across Kotlin and Python codebases to role/capability-based terminology (`Remote`, `Local`, `Network`, `Blob`, `Email`) per the `AGENTS.md` interface-first naming policy.

All violations recorded in `docs/interface-naming-baseline.txt` have been eradicated without any deprecation baggage, shrinking the baseline to zero entries.

## 2. Changes Implemented

### 1. Kotlin Model Enums (`frontend/core/data` & `frontend/core/artifacts`)
- Renamed `toFirestore()` → `toRemote()` and `fromFirestore()` → `fromRemote()` on:
  - `TrackerArtifactKind` in `frontend/core/artifacts/model/`
  - 10 core data models in `frontend/core/data/model/`: `ApplicationChannel`, `Confidence`, `DerivedFrom`, `EventActor`, `RoleLocation`, `TrackerEditKind`, `TrackerEditState`, `TrackerEventType`, `TrackerStatus`, `Trigger`.
- Updated all call sites in `ArtifactsRepositoryImpl`, `CompanyDetailsRepositoryImpl`, `RemoteJobApplicationDetailsDataSourceImpl`, `RemoteJobApplicationsListDataSourceImpl`, and `EnumLogicTest`.

### 2. Local Database & Remote Providers
- Renamed `createRoomDatabase()` → `createLocalDatabase()` in `frontend/core/data/database/DatabaseBuilder.kt` and updated DI wiring in `CoreDataModule.kt`.
- Renamed `FirestoreProvider.kt` → `RemoteDataSourceProvider.kt` in `frontend/apps/cli/db/` and updated property `firestore` → `remote` across all 13 CLI commands.
- Renamed `firestoreInstance` → `remoteInstance` in `frontend/apps/terminal/.../TrackerApp.kt`.

### 3. Backend & Convention Plugins
- Renamed `BackendService.firestore` → `remoteDatabase` in `backend/service/.../BackendService.kt`.
- Renamed `ComposioWebhookRoute.kt` → `EmailWebhookRoute.kt` and `Route.composioWebhookRoute()` → `Route.emailWebhookRoute()`.
- Renamed `FirebaseDependenciesConventionPlugin.kt` → `RemoteDependenciesConventionPlugin.kt` in `gradle/tools/project-plugins/`.

### 4. Records Parser Bridge
- Renamed `TimestampConverter`: `toFirestore()` → `toRemote()` and `fromFirestore()` → `fromRemote()`.
- Renamed `JPypeBridge.kt` → `PythonBridge.kt` and refactored methods to `toRemote()` and `fromRemote()`.

### 5. Python Scripts
- `scripts/contract.py` (and `lib/contract.py`, `lib/tracker/contract.py`): renamed `toFirestore()` → `to_remote()` and `fromFirestore()` → `from_remote()`. Updated all call sites across `scripts/` and `lib/`.
- Renamed scripts to role-first filenames:
  - `scripts/backup_firestore.py` → `scripts/remote_backup.py`
  - `scripts/restore_firestore.py` → `scripts/remote_restore.py`
  - `scripts/gmail_fetch.py` → `scripts/email_fetch.py` (and function `composioBin` → `emailClientBin`)
  - `scripts/migrate_gcs.py` → `scripts/blob_migrate.py` (and functions `build_remote_index`, `build_blob_path`, `print_blob_tree`)
  - `scripts/purge_gcs_legacy.py` → `scripts/blob_purge_legacy.py`
  - `scripts/batch_relocate_gcs.py` → `scripts/blob_batch_relocate.py`
  - `scripts/relocate_gcs_blobs.py` → `scripts/blob_relocate.py`
  - `scripts/relocate_gcs_role_slugs.py` → `scripts/blob_relocate_role_slugs.py`
  - `scripts/sync_local_artifacts.py`: renamed `get_firestore` → `get_remote`
  - `scripts/resume_migration.py`: renamed `load_firestore_applications` → `load_remote_applications` and `resolve_role_from_firestore` → `resolve_role_from_remote`
- Updated referencing mise tasks in `.config/mise/tasks/admin/`, `.config/mise/tasks/artifacts/`, and `.config/mise/tasks/email/`.

### 6. Baseline & Roadmap
- Regenerated `docs/interface-naming-baseline.txt` with zero entries.
- Marked `RM-012` as `DONE (100%)` in `ROADMAP.md`.

## 3. Verification
- `mise run dev:guards` verified clean with 0 baseline entries.
- `python3 -m unittest scripts.tests.test_contract.TestFirestoreRoundTrip` verified all round-trip conversions pass.
- `mise run roadmap:status` confirmed 8/12 items done (78% completion).
