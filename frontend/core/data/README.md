# Module: `:core-data`

> [!NOTE]
> **Quick Facts for AI Agents & Developers**
> - **Subproject**: `:core-data`
> - **Package Root**: `dev.fnc314.tools.jobtracker.core.data`
> - **Primary Contributions**: Domain entities, encapsulated enums, `Factory`, Firestore/Ktor repository interfaces & multiplatform implementations, `AppSettings`
> - **Key Build/Verification Command**: `./gradlew :frontend:core-data:compileCommonMainKotlinMetadata` (or `./gradlew :frontend:core-data:jvmTest`)

---

## 1. Module Overview
`:core-data` serves as the foundational data and domain layer contract for the entire Job Tracker Kotlin Multiplatform application. It defines the universal domain models, Firestore schema mappings, serialization formats, repository contracts, and platform-specific storage engines.

---

## 2. Contribution to the Larger Application
All feature modules (`features/*`) depend on `:core-data` for data models and repository contracts. It ensures absolute domain uniformity across Android, Desktop (JVM), Web (Wasm/JS), and iOS targets:
- **Upstream Consumers**: `core:widgets`, `features:*`, `apps:core`.
- **Downstream Dependencies**: `core:network`, `kotlinx-datetime`, `kotlinx-coroutines-core`, `kotlinx-serialization-json`.

---

## 3. Package & Subproject Structure

```
core/data/src/
├── commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/
│   ├── di/CoreDataModule.kt                  # Koin DI module providing globalJson, SettingsStorage, AppSettingsRepository, PlatformFileOpener
│   ├── factory/Factory.kt                    # Centralized factory for creating validated domain models
│   ├── json/JsonProvider.kt                  # Standard globalJson instance for kotlinx.serialization
│   ├── model/                                # Universal entities and encapsulated enums
│   │   ├── JobApplication.kt                 # Core job application entity (schemaVersion 7, Uuid7)
│   │   ├── Uuid7.kt                          # Strongly-typed RFC 4122 UUIDv7 value class
│   │   ├── TrackerEvent.kt                   # Historical lifecycle event
│   │   ├── TrackerArtifact.kt                # Associated file artifact (CV, cover letter)
│   │   ├── TrackerEdit.kt                    # Proposed edit staging record
│   │   ├── Company.kt                        # Company profile and metadata
│   │   ├── TrackerStatus.kt                  # Funnel status enum with self-contained display & colors
│   │   ├── ApplicationChannel.kt             # Application source channel
│   │   ├── TrackerEventType.kt               # Timeline event types with icon mapping
│   │   ├── TrackerArtifactKind.kt            # Artifact classification (CV, Cover Letter, etc.)
│   │   ├── WorkMode.kt                       # Remote, Hybrid, On-site
│   │   ├── Trigger.kt, Confidence.kt, etc.   # Audit and edit provenance enums
│   │   ├── EnumDisplayExtensions.kt          # Generic display name fallback
│   │   └── WireContract.kt                   # WireContract + toWireLookupMap helpers (sealed Known/Unrecognized enums)
│   ├── database/                             # Room KMP Relational Persistence Engine
│   │   ├── AppDatabase.kt                    # Room database definition
│   │   ├── DatabaseBuilder.kt                # Expect/actual multiplatform database builder
│   │   ├── DatabaseConverters.kt             # Room 3 ColumnTypeConverter implementations
│   │   ├── DatabaseHolder.kt                 # Thread-safe database & DAO container
│   │   ├── JobApplicationMapper.kt           # Bidirectional entity <-> domain mapper
│   │   ├── dao/JobApplicationDao.kt          # DAO for querying and persisting applications
│   │   └── entity/JobApplicationEntity.kt    # SQLite entity definition (schemaVersion 7)
│   ├── platform/PlatformFileOpener.kt        # Multiplatform file/PDF opener contract
│   ├── repository/                           # Base domain abstractions
│   │   └── UseCase.kt                        # Base functional use case interface
│   ├── settings/                             # Preferences persistence
│   │   ├── AppSettings.kt                    # App configuration model (UiMode, seed colors, etc.)
│   │   └── SettingsStorage.kt                # Multiplatform key-value settings storage
│   ├── source/                               # Data Source Engines
│   │   ├── LocalDataSource.kt                # Key-value local engine contract & implementation
│   │   ├── NetworkDataSource.kt              # Ktor HTTP network engine contract & implementation
│   │   ├── RemoteDataSource.kt               # Provider-agnostic remote database contract
│   │   ├── RemoteRecord.kt                   # Typed document record abstraction
│   │   └── DataResult.kt                     # Success and Failure result wrapper
│   └── util/                                 # Semantic timestamp formatting
├── androidMain/kotlin/                       # Android SharedPreferences & PlatformFileOpener bindings
├── jvmMain/kotlin/                           # Desktop JVM Preferences & PlatformFileOpener bindings
├── iosMain/kotlin/                           # iOS Darwin NSUserDefaults & PlatformFileOpener bindings
└── webMain/kotlin/                           # Web LocalStorage & PlatformFileOpener bindings
```

---

## 4. Public API Surface & Significant Contributions

### Core Models & Factory
- `Factory`: Central factory providing `createJobApplication()`, `createTrackerEvent()`, `createTrackerArtifact()`, and `createTrackerEdit()` with automated timestamping, default `schemaVersion = 7`, and ID/UUIDv7 assignment.
- `JobApplication`: Complete immutable representation of an application record with `schemaVersion = 7`, monotonic `uuid: Uuid7`, slug `id`, fit score, status, channels, notes, and tags.
- `Uuid7`: Type-safe inline value class wrapping standard library `kotlin.uuid.Uuid` (native `generateV7()`) providing time-ordered monotonicity and zero memory allocation overhead.
- `AppDatabase`: Room 3.x multiplatform SQLite relational database with `@ColumnTypeConverters`, `JobApplicationDao`, `JobApplicationMapper`, and platform-specific builders (`androidMain`, `jvmMain`, `iosMain`, `wasmJsMain`, `jsMain`).

### Self-Contained Open Enums (`WireContract`)
All domain enums implement `WireContract` as sealed interface hierarchies (`Known` enum + `Unrecognized(wireKey)` carrier for lossless forward-compatible wire parsing) with O(1) lookup maps built via `Iterable<E>.toWireLookupMap()`:
- `TrackerStatus`: Contains `.displayName()`, `.toFirestore()`, `fromWireKey()` / `fromFirestore()`, `@Composable fun containerColor()`, `@Composable fun contentColor()`, `.isTerminal`, and `.isActive`.
- `TrackerEventType`: Contains `.icon`, `.displayName()`, `.toFirestore()`, and `fromWireKey()` / `fromFirestore()`.
- `ApplicationChannel`: Self-contained channel categorization and parsing (including outreach channels).
- `WorkMode`, `Confidence`, `DerivedFrom`, `EventActor`, `TrackerArtifactKind`, `TrackerEditKind`, `TrackerEditState`, `Trigger`: Audit, provenance, and workflow sealed hierarchies.
- `SyncKind`, `SyncState`, `Region`, `Tag`, `CompPeriod`, `CompVsTarget`: Synchronization, geographic region, controlled vocabulary tag, and compensation sub-enum sealed hierarchies.

### Settings & Preferences
- `AppSettings`: Data model capturing app theme mode (`UiMode`), seed colors, and preferences.
- `AppSettingsRepository`: Reactive repository providing hot `StateFlow<AppSettings>` and persistence via `SettingsStorage`.
- `SettingsStorage`: Multiplatform key-value settings storage contract and implementations (SharedPreferences, Preferences API, NSUserDefaults, LocalStorage).
- `RemoteDataSource`: Multiplatform contract defining remote database interactions (`observeCollection`, `getDocument`, `setDocument`, `updateDocument`) via `createRemoteDataSource()`, backed by platform-native Firebase Firestore integrations (`AndroidRemoteDataSource` and `JvmRemoteDataSource`).
- `AppDatabase`: Room 3.x multiplatform SQLite relational database and DAO contract. Feature modules consume these engines through their own domain repositories and feature data sources.

---

## 5. Key Gradle & Mise Tasks

| Task | Scope | Purpose |
| :--- | :--- | :--- |
| `./gradlew :frontend:core-data:compileCommonMainKotlinMetadata` | Module | Rapid metadata compilation and explicit API verification |
| `./gradlew :frontend:core-data:jvmTest` | Module | Executes unit tests for parsing, factories, and settings |
| `./gradlew :frontend:core-data:assemble` | Module | Compiles all multiplatform library targets (Android, JVM, iOS, Web) |
| `mise run frontend:test` | Workspace | Runs tests across all core data and shared modules |

---

## 6. Deep-Link & Navigation Integration
`:core-data` is a data and domain foundation. It does not provide Compose navigation destinations or UI screens, but defines the underlying entities (`JobApplication.id`) that drive navigation parameters (`DetailRoute(val applicationId: String)`).

---

## 7. Convention Plugins & Dependencies

### Applied Convention Plugins
- `local.kotlin.multiplatform`: Multi-target toolchain (JVM, Android, iOS, Web).
- `local.android.kmp.library`: AGP Multiplatform Library setup with `compileSdk = 37`.
- `local.compose`: Compose Multiplatform runtime for `@Composable` enum color lookups.
- `local.dependencies.compose`: Compose UI and Foundation dependencies.
- `local.dependencies.serialization`: Kotlinx Serialization plugin and JSON runtime.

### Key Dependencies
- `kotlinx.coroutines.core`: Coroutines for reactive repository flows.
- `kotlinx.datetime`: Instant and Clock support for application timelines.
- `bundles.ktor.client`: HTTP client for cloud artifact access.
- `bundles.firebase.android` / `bundles.firebase.jvm`: Firestore SDK bindings.
