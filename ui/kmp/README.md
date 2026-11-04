# FNC314 Job Search Tracker — Compose Multiplatform (KMP)

A cross-platform native job search and application tracker built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, targeting **Android, Desktop (macOS/JVM), Web (Wasm & JS), and iOS**.

This client interfaces directly with the central Firestore system of record (`job-search-fnc314-dev`), adhering strictly to the schema and append-only event contracts defined in `tracker.schema.json`.

---

## Architecture & Project Hierarchy

The project uses Gradle's type-safe project accessors, convention plugins (`project-collections-gradle-settings-plugin`), and modular architecture across core, feature, app, and shared layers:

```
ui/kmp/
├── apps/                               # Platform entry points
│   ├── android/                        # Android application (Jetpack Compose Material 3, Firebase)
│   ├── desktop/                        # Desktop JVM application (Compose Multiplatform)
│   ├── web/                            # Web application (Kotlin/Wasm & Kotlin/JS targets)
│   └── ios/                            # iOS Xcode project & SwiftUI wrappers
├── core/                               # Cross-cutting foundational modules
│   ├── data/                           # Data models and repository contracts
│   ├── design-system/                  # Material 3 theme, tokens, typography, and component styling
│   ├── network/                        # HTTP/REST clients and serialization
│   └── widgets/                        # Reusable UI widgets (cards, chips, list rows)
├── features/                           # Feature modules (isolated domain/data/UI)
│   └── sign-in/
│       ├── ui/                         # Authentication UI screens & viewmodels
│       ├── domain/                     # Sign-in use cases & business rules
│       └── data/                       # Auth repository implementations
├── shared/                             # Core cross-platform domain & business logic
│   ├── commonMain/                     # Shared models (JobApplication, TrackerEvent), use cases, Koin DI
│   ├── androidMain/                    # Android-specific implementations (AndroidFirestoreJobApplicationRepository)
│   ├── jvmMain/                        # JVM desktop implementations (JVMJobApplicationRepository)
│   ├── wasmJsMain/ & jsMain/           # Web platform implementations
│   └── iosMain/                        # iOS platform implementations
└── records/                            # Included build: Protobuf definitions & parser bridge
    ├── proto/                          # Protobuf schemas (tracker/v1/application.proto)
    └── parser/                         # Data parser and type conversion bridge
```

---

## Tooling & Mise Integration

All significant Gradle workflows can be invoked directly from the repository root via the `mise` task runner (`mise run ui:kmp:<task>`), or directly from this directory using `./gradlew`.

### Mise Tasks (Repo Root)

| Goal | Mise Command | Underlying Gradle Task |
|---|---|---|
| **Build all** | `mise run ui:kmp:build` | `./gradlew build -x test` |
| **Run tests** | `mise run ui:kmp:test` | `./gradlew :shared:jvmTest` |
| **Clean cache** | `mise run ui:kmp:clean` | `./gradlew clean` |
| **Desktop Run** | `mise run ui:kmp:desktop:run` | `./gradlew :apps-desktop:run` |
| **Desktop Package** | `mise run ui:kmp:desktop:package` | `./gradlew :apps-desktop:packageDistributionForCurrentOS` |
| **Android Assemble** | `mise run ui:kmp:android:assemble` | `./gradlew :apps-android:assembleDebug` |
| **Android Bundle** | `mise run ui:kmp:android:bundle` | `./gradlew :apps-android:bundleRelease` |
| **Android Install** | `mise run ui:kmp:android:install` | `./gradlew :apps-android:installDebug` |
| **Web Wasm Dev** | `mise run ui:kmp:web:wasm:dev` | `./gradlew :apps-web:wasmJsBrowserDevelopmentRun` |
| **Web Wasm Dist** | `mise run ui:kmp:web:wasm:dist` | `./gradlew :apps-web:wasmJsBrowserDistribution` |
| **Web JS Dev** | `mise run ui:kmp:web:js:dev` | `./gradlew :apps-web:jsBrowserDevelopmentRun` |
| **Web JS Dist** | `mise run ui:kmp:web:js:dist` | `./gradlew :apps-web:jsBrowserDistribution` |

---

## Platform Details & Execution

### 1. Desktop (JVM)
The desktop target produces a native macOS application with full menu bar integration, keyboard shortcuts, and direct connection to Firestore.
```bash
# Run locally
./gradlew :apps-desktop:run

# Package native bundle (.dmg on macOS, .msi on Windows, .deb on Linux)
./gradlew :apps-desktop:packageDistributionForCurrentOS
```

### 2. Android
The Android application uses Jetpack Compose Material 3 and Firebase Authentication / Cloud Firestore.
```bash
# Compile debug APK
./gradlew :apps-android:assembleDebug

# Install directly to an active emulator or USB-connected device
./gradlew :apps-android:installDebug
```

### 3. Web (Wasm & JS)
The Web app compiles using Compose Multiplatform for Web:
```bash
# Development server with hot reload (Wasm)
./gradlew :apps-web:wasmJsBrowserDevelopmentRun

# Build production bundle
./gradlew :apps-web:wasmJsBrowserDistribution
```

### 4. iOS
The iOS app uses the shared Kotlin framework compiled for Apple Silicon / iOS Simulator targets:
- Open `apps/ios/iosApp.xcworkspace` (or `apps/ios`) in Xcode.
- Select your target simulator (e.g. iPhone 16 Pro) and press **Cmd+R** to build and run.
- Tests can be run via Gradle:
  ```bash
  ./gradlew :shared:iosSimulatorArm64Test
  ```

---

## Data Model & Firestore Integrity

The KMP client communicates with Cloud Firestore under the strict invariants enforced by the root repository:
1. **Append-Only Event History**: Status updates, notes, and activity changes must be logged as events in the `applications/{id}/events` subcollection.
2. **Schema Conformance**: Document fields must map exactly to `tracker.schema.json` (e.g., `fitRating` instead of `fitScore`; `channel:outreach:linkedin` for namespaced channels).
3. **Derived Projections**: Display fields (`notesRendered`, `counters`, `timeline`) are treated as derived projections of the underlying event log.
