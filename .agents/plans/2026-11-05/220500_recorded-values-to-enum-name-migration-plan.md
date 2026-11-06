# Architecture Plan: Recorded Values to Exact `Enum.name` Alignment (Schema v8)

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** `records` and `ui/kmp` (cross-subsystem)  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  

---

## 1. Executive Summary & Review Feedback Alignment

Following user feedback on the initial plan, we refine the naming and migration strategy to achieve complete alignment across `records/proto`, `records/parser`, and `ui/kmp`:

1. **`WorkMode` Renaming & Value Clarification (L32):**
   - Replace the ambiguous `WorkMode` concept with **`RoleLocation`**:
     ```kotlin
     enum class RoleLocation {
         ONSITE,
         HYBRID,
         REMOTE,
     }
     ```
   - (Or if scoped with prefix: `WORK_ONSITE`, `WORK_HYBRID`, `WORK_REMOTE`).
   - In Kotlin, `RoleLocation { ONSITE, HYBRID, REMOTE }` is concise, unambiguous, and semantically represents the location modality of the role.
2. **Confidence Nomenclature (L39):**
   - Use `HIGH_CONFIDENCE`, `MEDIUM_CONFIDENCE`, `LOW_CONFIDENCE` (or `CONFIDENCE_HIGH`, etc.) rather than naked `HIGH`/`LOW` strings, avoiding collisions and giving clear semantic context.
3. **Region Nomenclature (L41):**
   - Adopt `US_` prefixes for US geographical regions and remote designations:
     - `US_PIT`, `US_NYC`, `US_SF`, `US_LA`, `US_DC`, `US_REMOTE` (or `US_REMOTE_ALL`).
4. **Single-User Offline Backup & Clean One-Shot Migration (L103):**
   - Since there is only one user, we don't need complex zero-downtime dual-read rolling migrations.
   - We will back up the Firestore collection offline as JSON, run a single deterministic transformation script over the data, and batch write/replace the updated documents in Firestore in one smooth operation.
5. **Schema Versioning (`schemaVersion = 8`) (L111):**
   - Yes! The repository strictly versions its definitions via `schemaVersion` in [`tracker.schema.json`](../../tracker.schema.json).
   - The current contract is `schemaVersion: 7`.
   - This migration will formally bump the contract to **`schemaVersion: 8`** with an entry in `"x-revision-log"` documenting the transition of all recorded values to exact `Enum.name` strings.

---

## 2. Updated Canonical Enum Specification (`Enum.name`)

| Proto / KMP Enum | Current Wire Values | Updated Exact `Enum.name` (Firestore & Wire) | Notes / Refinements |
| :--- | :--- | :--- | :--- |
| **`TrackerStatus`** | `status:lead`, `status:applied`, `status:interview`, `status:offer`, `status:hired`, `status:rejected`, `status:no-response`, `status:offer-declined`, `status:withdrawn` | `LEAD`, `DRAFTING`, `APPLIED`, `INTERVIEW`, `OFFER_EXPECTED`, `HIRED`, `REJECTED`, `NO_RESPONSE`, `OFFER_DECLINED`, `APPLICATION_WITHDRAWN` | Exact match with Kotlin `Enum.name` and Proto |
| **`TrackerEventType`** | `event:applied`, `event:recruiter-screen`, `event:interview-scheduled`, etc. | `APPLY`, `ACKNOWLEDGE`, `RECRUITER_SCREEN`, `SCHEDULE_INTERVIEW`, `COMPLETE_INTERVIEW`, `ASSESS`, `VISIT_ONSITE`, `OFFER`, `ACCEPT_OFFER`, `DECLINE_OFFER`, `REJECT`, `GHOST`, `WITHDRAW`, `SEND_FOLLOW_UP`, `TAILOR_DOCUMENT`, `RECALCULATE_FIT`, `ADD_ARTIFACT`, `ADD_NOTE`, `IMPORT`, `CORRECT_STATUS`, `RECRUITER_OUTREACH` | Action verbs matching Proto & KMP |
| **`ApplicationChannel`** | `channel:greenhouse`, `channel:outreach:linkedin`, etc. | `GREENHOUSE`, `WORKDAY`, `LEVER`, `ASHBY`, `SMARTRECRUITERS`, `LINKEDIN`, `REFERRAL`, `DIRECT`, `OTHER`, `OUTREACH_LINKEDIN`, `OUTREACH_EMAIL`, `OUTREACH_REFERRAL` | Direct exact match |
| **`RoleLocation`** *(formerly `WorkMode`)* | `work-mode:onsite`, `work-mode:hybrid`, `work-mode:remote` | `ONSITE`, `HYBRID`, `REMOTE` *(or `WORK_ONSITE`, `WORK_HYBRID`, `WORK_REMOTE`)* | Renamed from `WorkMode` to `RoleLocation` |
| **`TrackerArtifactKind`** | `artifact:cv`, `artifact:cover-letter`, `artifact:outcome`, `artifact:posting`, `artifact:assessment` | `CV`, `COVER_LETTER`, `OUTCOME`, `POSTING`, `ASSESSMENT` | Matches KMP `Enum.name` |
| **`EventActor`** | `actor:user`, `actor:agent`, `actor:gmail-sync`, `actor:import`, `actor:scheduler` | `USER`, `AGENT`, `EMAIL_SYNC`, `IMPORT`, `SCHEDULER` | Matches KMP `Enum.name` |
| **`TrackerEditKind`** | `proposal:gmail-sync`, `proposal:sheet-sync`, `proposal:lead` | `EMAIL_SYNC`, `SHEET_SYNC`, `LEAD` | Matches KMP `Enum.name` |
| **`TrackerEditState`** | `proposal:pending`, `proposal:approved`, etc. | `PENDING`, `APPROVED`, `REJECTED`, `EXPIRED` | Matches KMP `Enum.name` |
| **`Trigger`** | `trigger:cron`, `trigger:manual`, etc. | `CRON`, `APPLICATION_CREATED`, `APPLICATION_UPDATED`, `MANUAL` | Matches KMP `Enum.name` |
| **`Confidence`** | `high`, `medium`, `low` | `HIGH_CONFIDENCE`, `MEDIUM_CONFIDENCE`, `LOW_CONFIDENCE` | Updated per user feedback |
| **`DerivedFrom`** | `manual`, `notes-regex`, `gmail`, `csv-import` | `MANUAL`, `NOTES_REGEX`, `EMAIL`, `CSV_IMPORT` | Matches KMP `Enum.name` |
| **`Region`** | `PIT`, `NYC`, `SF`, `LA`, `DC`, `REMOTE-US` | `US_PIT`, `US_NYC`, `US_SF`, `US_LA`, `US_DC`, `US_REMOTE` | Updated per user feedback with `US_` prefix |
| **`Tag`** | `relocation:required`, `compensation:above-band`, etc. | `RELOCATION_REQUIRED`, `COMPENSATION_ABOVE_BAND`, `FIT_STRONG`, `PROCESS_TAKE_HOME`, etc. | Controlled vocabulary tags |
| **`ScrapedPostingStatus`** | `NEW`, `REVIEWED`, `REJECTED`, `APPLIED` | `NEW`, `REVIEWED`, `REJECTED`, `APPLIED` | Already an exact match |

---

## 3. Decision Matrix: `RoleLocation` Formats

We have two options for `RoleLocation` enum names:
- **Option A (Pure Semantic):**
  ```kotlin
  enum class RoleLocation {
      ONSITE,
      HYBRID,
      REMOTE,
  }
  ```
  In Firestore: `"roleLocation": "REMOTE"`.
- **Option B (Prefix-Scoped):**
  ```kotlin
  enum class RoleLocation {
      WORK_ONSITE,
      WORK_HYBRID,
      WORK_REMOTE,
  }
  ```
  In Firestore: `"roleLocation": "WORK_REMOTE"`.

*Recommendation:* Option A (`ONSITE`, `HYBRID`, `REMOTE`) is cleaner when nested under `Location` (`location.roleLocation = REMOTE`), but Option B prevents any potential ambiguity if ever flattened. We will adopt Option A unless the user explicitly prefers `WORK_ONSITE`.

---

## 4. Single-User Migration Execution Strategy

Since there are no concurrent users:
1. **Local Backup:**
   - Run a backup script: `python3 scripts/backup_firestore.py` → saves all Firestore collections (`applications`, `events`, `artifacts`, `proposals`, etc.) to `generated/backup_v7_<timestamp>.json`.
2. **Schema & Code Updates (v8):**
   - Update `tracker.schema.json` to `schemaVersion: 8`.
   - Update `records/proto`, recompile protos (`mise run records:proto:generate`).
   - Update `records/parser` and `ui/kmp` domain models (`wireKey = this.name`).
   - Update Python modules (`scripts/contract.py`, `scripts/events.py`, etc.).
3. **Data Transformation & Restore:**
   - Execute a batch offline migration script that transforms the backup JSON into v8 format.
   - Batch write the v8 documents back to Firestore with `schemaVersion: 8`.
4. **Verification:**
   - Run test suite: `mise run tracker check`, KMP unit tests, and CLI queries.

---

## 5. Proposed File Modifications

### Schema & Proto
- **`tracker.schema.json`**:
  - Bump `"schemaVersion": 8`.
  - Add revision note in `"x-revision-log"`.
  - Rename `workMode` to `roleLocation` (or keep key with new enum values).
  - Update all `$defs/*Value` arrays to the new uppercase `Enum.name` values.
- **`records/proto/src/main/proto/tracker/v1/enums.proto`**:
  - Rename `WorkMode` → `RoleLocation` (`ONSITE = 1`, `HYBRID = 2`, `REMOTE = 3`).
  - Update `Confidence` → `HIGH_CONFIDENCE`, etc.
  - Update `Region` → `US_PIT`, `US_NYC`, `US_SF`, `US_LA`, `US_DC`, `US_REMOTE`.
- **`records/proto/src/main/proto/tracker/v1/location.proto`**:
  - Update field `RoleLocation roleLocation = 4;` (replacing `WorkMode workMode`).

### Records & Parser
- **`records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/ProtoWireContracts.kt`**:
  - Set all `.wireKey` to exact `this.name`.
- **`records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/StatusNormalizer.kt` & `LegacyRowProjector.kt`**:
  - Update legacy projection mappings.

### Kotlin Multiplatform (`ui/kmp`)
- **`ui/kmp/core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/`**:
  - Replace `WorkMode.kt` with `RoleLocation.kt`.
  - Update `Confidence.kt` and `Region.kt`.
  - Update `wireKey = this.name` across all sealed interfaces.
  - Bump `schemaVersion = 8` in `JobApplication.kt`, `Factory.kt`, and `JobApplicationEntity.kt`.

### Python Scripts & CLI
- **`scripts/contract.py`**, **`scripts/events.py`**, **`scripts/tracker.py`**:
  - Align status ranks and event mapping to exact `Enum.name` strings.
- **`scripts/migrate_v8.py`**:
  - One-shot Firestore backup, transform, and update script.

---

## 6. Verification Plan

1. **Protobuf & Parser Build**:
   ```bash
   mise run records:proto:generate
   cd records && ./gradlew :records-parser:test
   ```
2. **KMP JVM & Multiplatform Tests**:
   ```bash
   cd ui/kmp && ./gradlew :core:data:jvmTest :apps:core:jvmTest
   ```
3. **Python Contract & CLI Tests**:
   ```bash
   python3 -m unittest discover -s scripts/tests -p "test_*.py"
   mise run tracker check --offline
   ```
