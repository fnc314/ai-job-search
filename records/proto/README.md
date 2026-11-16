# Records Proto: Protobuf Contracts

Protocol Buffer schemas for the Job Application Tracker domain model, compiling to pure Kotlin types via `protoc-gen-kotlin`.

## Overview

Contains the `tracker/v1` protobuf schema definitions governing:
- `application.proto`: Core application record structure and metadata.
- `status.proto`: Pipeline states and transitions.
- `event.proto`: Append-only event history.
- `edit.proto`: Auditor mutation log.
