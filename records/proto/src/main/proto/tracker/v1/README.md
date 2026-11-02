# Tracker v1 Protobuf Definitions

Proto3 definitions for the job tracker data model, mirroring `tracker.schema.json`.

## Package Structure

All protos live under `package tracker.v1` with Java package `dev.fnc314.tools.jobsearch.tracker.v1`.

## Files

| Proto File | Message | Firestore Collection |
|---|---|---|
| `enums.proto` | 15 enums | Shared across all types |
| `compensation.proto` | `Compensation` | Nested in `JobApplication` |
| `location.proto` | `Location` | Nested in `JobApplication` |
| `fit_evaluation.proto` | `TrackerFit` | Nested in `JobApplication` |
| `counters.proto` | `TrackerCounts` | Nested in `JobApplication` |
| `timeline.proto` | `Timeline` | Nested in `JobApplication` |
| `application.proto` | `JobApplication` | `applications/{id}` |
| `event.proto` | `TrackerEvent` | `applications/{id}/events/{id}` |
| `artifact.proto` | `TrackerArtifact` | `applications/{id}/artifacts/{id}` |
| `edit.proto` | `TrackerEdit` | `edits/{id}` |
| `sync_request.proto` | `TrackerSyncRequest` | `sync-requests/{id}` |
| `company.proto` | `Company` | `companies/{id}` |
| `contact.proto` | `Contact` | `contacts/{id}` |

## Enum Naming Convention

Proto enum values use semantic names to avoid package-level collisions (proto3 uses flat package-level scoping for all enum values). Statuses keep canonical state names, events use present-tense action verbs:

| Schema Value | Proto Enum | Proto Value |
|---|---|---|
| `status:applied` | `TrackerStatus` | `APPLIED` |
| `status:offer` | `TrackerStatus` | `OFFER_EXPECTED` |
| `status:withdrawn` | `TrackerStatus` | `APPLICATION_WITHDRAWN` |
| `event:interview-scheduled` | `TrackerEventType` | `SCHEDULE_INTERVIEW` |
| `event:offer-declined` | `TrackerEventType` | `DECLINE_OFFER` |
| `channel:greenhouse` | `ApplicationChannel` | `GREENHOUSE` |
| `artifact:cv` | `TrackerArtifactKind` | `ARTIFACT_CV` |
| `work-mode:remote` | `WorkMode` | `WORK_MODE_REMOTE` |
| `actor:email-sync` | `EventActor` | `ACTOR_EMAIL_SYNC` |
| `proposal:pending` | `TrackerEditState` | `EDIT_STATE_PENDING` |
| `trigger:manual` | `Trigger` | `TRIGGER_MANUAL` |
| `sync:pending` | `SyncState` | `SYNC_STATE_PENDING` |

## Timestamps

All timestamp fields use `google.protobuf.Timestamp` instead of ISO date strings. Use `TimestampConverter.kt` in the parser module to convert between Firestore timestamps and proto Timestamps.

## Build

```bash
# Generate Kotlin classes from protos
cd records && ./gradlew :records-proto:generateProto

# Build all modules
cd records && ./gradlew assemble

# Via mise
mise run records:build
mise run records:proto:generate
```

## Python Bindings

Python can consume these protos via `grpcio-tools`:

```bash
python -m grpc_tools.protoc -Irecords/proto/src/main/proto \
  --python_out=generated/ \
  records/proto/src/main/proto/tracker/v1/*.proto
```
