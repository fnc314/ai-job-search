# Module: `:core-data`

> [!NOTE]
> **Quick Facts for AI Agents & Developers**
> - **Subproject**: `:core-data`
> - **Package Root**: `dev.fnc314.tools.jobtracker.core.data`
> - **Primary Contributions**: Domain entities, encapsulated enums, `Factory`, Firestore/Ktor repository interfaces & multiplatform implementations, `AppSettings`
> - **Key Build/Verification Command**: `./gradlew :core-data:compileCommonMainKotlinMetadata` (or `./gradlew :core-data:jvmTest`)

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
│   ├── factory/Factory.kt                    # Centralized factory for creating validated domain models
│   ├── model/                                # Universal entities and encapsulated enums
│   │   ├── JobApplication.kt                 # Core job application entity
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
│   │   └── EnumParsing.kt                    # Firestore string parsing
│   ├── platform/PlatformFileOpener.kt        # Multiplatform file/PDF opener contract
│   ├── repository/                           # Storage and database abstractions
│   │   ├── JobApplicationRepository.kt       # Application and event data store contract
│   │   ├── StorageRepository.kt              # Artifact storage contract
│   │   └── KtorStorageRepository.kt          # Signed-URL artifact downloader
│   ├── settings/                             # Preferences persistence
│   │   ├── AppSettings.kt                    # App configuration model (UiMode, seed colors, etc.)
│   │   └── SettingsStorage.kt                # Multiplatform key-value settings storage
│   └── util/                                 # Semantic timestamp formatting
├── androidMain/kotlin/                       # Android Firestore repository & Play Services bindings
├── jvmMain/kotlin/                           # Desktop JVM Firestore repository & protobuf parser
├── iosMain/kotlin/                           # iOS Darwin storage & platform bindings
└── webMain/kotlin/                           # Web JS/Wasm storage bindings
```

---

## 4. Public API Surface & Significant Contributions

### Core Models & Factory
- `Factory`: Central factory providing `createJobApplication()`, `createTrackerEvent()`, `createTrackerArtifact()`, and `createTrackerEdit()` with automated timestamping and ID assignment.
- `JobApplication`: Complete immutable representation of an application record with fit score, status, channels, notes, and tags.

### Self-Contained Enums (Zero External When)
All enums encapsulate their own Firestore mappings, display strings, and semantic Material 3 tonal colors:
- `TrackerStatus`: Contains `.displayName()`, `.toFirestore()`, `fromFirestore()`, `@Composable fun containerColor()`, `@Composable fun contentColor()`, `.isTerminal`, and `.isActive`.
- `TrackerEventType`: Contains `.icon`, `.displayName()`, `.toFirestore()`, and `fromFirestore()`.
- `ApplicationChannel`: Self-contained channel categorization and parsing.

### Repositories
- `JobApplicationRepository`: Suspend contract for loading, querying, creating, and updating job applications and appending timeline events.
- `StorageRepository`: Contract for retrieving signed URLs and downloading artifacts from Cloud Storage.

---

## 5. Key Gradle & Mise Tasks

| Task | Scope | Purpose |
| :--- | :--- | :--- |
| `./gradlew :core-data:compileCommonMainKotlinMetadata` | Module | Rapid metadata compilation and explicit API verification |
| `./gradlew :core-data:jvmTest` | Module | Executes unit tests for parsing, factories, and settings |
| `./gradlew :core-data:assemble` | Module | Compiles all multiplatform library targets (Android, JVM, iOS, Web) |
| `mise run ui:kmp:test` | Workspace | Runs tests across all core data and shared modules |

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
