# Records Subsystem: Protocol Buffers & Contract Parsers

The `records` subsystem serves as the strongly-typed contract and schema foundation for the AI Job Search & Application OS across all Kotlin and JVM runtimes.

## Modules

- **`:records:records-proto`**: Pure Protocol Buffer definitions (`tracker/v1/*.proto`) generating Kotlin domain models for applications, status enums, timeline events, and audit edits.
- **`:records:records-parser`**: Domain parsing logic, timestamp normalization, Firestore remote converters (`toRemote()` / `fromRemote()`), and legacy contract projectors.

## Architectural Principles

1. **Schema Authority**: The canonical contract mirrors `tracker.schema.json`.
2. **Interface-First Naming**: Public surfaces are named for their roles and capabilities (`toRemote()`, `fromRemote()`, `RemoteSync`), strictly avoiding underlying tool names.
3. **Zero Android Dependencies**: Records modules target pure JVM/Kotlin Multiplatform for broad cross-subsystem consumption.
