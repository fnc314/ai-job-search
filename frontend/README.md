# FNC314 Job Search Tracker — Compose Multiplatform (KMP)

A cross-platform native job search and application tracker built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, targeting **Android, Desktop (macOS/JVM), Web (Wasm & JS), and iOS**.

This client interfaces directly with the central Firestore system of record (`job-search-fnc314-dev`), adhering strictly to the schema and append-only event contracts defined in `tracker.schema.json`.

---

## Architecture & Project Hierarchy

The project uses Gradle's type-safe project accessors, convention plugins (`project-collections-gradle-settings-plugin`), and modular architecture across core, feature, app, and shared layers:

```
frontend/
├── apps/                               # Platform entry points
│   ├── android/                        # Android application (Jetpack Compose Material 3, Firebase)
│   ├── desktop/                        # Desktop JVM application (Compose Multiplatform)
│   ├── web/                            # Web application (Kotlin/Wasm & Kotlin/JS targets)
│   ├── ios/                            # iOS Xcode project & SwiftUI wrappers
│   ├── cli/                            # Job Tracker CLI companion (Clikt JVM)
│   ├── terminal/                       # Curses TUI tracker (Mordant/Lanterna JVM)
│   └── core/                           # Root application shell, dynamic AppBar & NavHost
├── core/                               # Cross-cutting foundational modules
│   ├── data/                           # Data models and repository contracts
│   ├── design-system/                  # Material 3 theme, tokens, typography, and component styling
│   ├── network/                        # HTTP/REST clients and serialization
│   ├── navigation/                     # Decoupled type-safe navigation contracts
│   └── widgets/                        # Reusable UI widgets (cards, chips, list rows)
└── features/                           # Consolidated feature modules (isolated domain/data/ui)
    ├── sign-in/                        # Authentication UI screens & viewmodels
    ├── job-applications-list/          # Pipeline overview and filterable application list
    ├── job-application-details/        # Comprehensive application dossier
    ├── add-job-application/            # Intake form and fast-lead creation
    ├── settings/                       # Theme, sync, and credential preferences
    └── tracker-edits/                  # Staged Gmail proposal review queue
```

---

## Tooling & Mise Integration

All significant Gradle workflows can be invoked directly from the repository root via the `mise` task runner (`mise run frontend:<task>`), or directly using `./gradlew`.

### Mise Tasks (Repo Root)

| Goal                 | Mise Command                       | Underlying Gradle Task                                    |
|----------------------|------------------------------------|-----------------------------------------------------------|
| **Build all**        | `mise run frontend:build`          | `./gradlew :frontend:build -x test`                       |
| **Run tests**        | `mise run frontend:test`           | `./gradlew :frontend:core-data:jvmTest :frontend:apps-core:jvmTest` |
| **Detekt analysis**  | `mise run frontend:detekt`         | Dynamic array iteration over all frontend submodules      |
| **Clean cache**      | `mise run frontend:clean`          | `./gradlew :frontend:clean`                               |
| **Desktop Run**      | `mise run frontend:desktop:run`    | `./gradlew :frontend:apps-desktop:run`                     |
| **Desktop Package**  | `mise run frontend:desktop:package`| `./gradlew :frontend:apps-desktop:packageDistributionForCurrentOS` |
| **Android Assemble** | `mise run frontend:android:assemble`| `./gradlew :frontend:apps-android:assembleDebug`          |
| **Android Bundle**   | `mise run frontend:android:bundle` | `./gradlew :frontend:apps-android:bundleRelease`          |
| **Android Install**  | `mise run frontend:android:install`| `./gradlew :frontend:apps-android:installDebug`           |
| **Web Wasm Dev**     | `mise run frontend:web:wasm:dev`   | `./gradlew :frontend:apps-web:wasmJsBrowserDevelopmentRun`|
| **Web Wasm Dist**    | `mise run frontend:web:wasm:dist`  | `./gradlew :frontend:apps-web:wasmJsBrowserDistribution`  |
| **Web JS Dev**       | `mise run frontend:web:js:dev`     | `./gradlew :frontend:apps-web:jsBrowserDevelopmentRun`    |
| **Web JS Dist**      | `mise run frontend:web:js:dist`    | `./gradlew :frontend:apps-web:jsBrowserDistribution`      |
| **Module Readmes**   | `mise run frontend:readmes`        | Validates module README presence and completeness         |

---

## Platform Details & Execution

### 1. Desktop (JVM)
The desktop target produces a native macOS application with full menu bar integration, keyboard shortcuts, and direct connection to Firestore.
```bash
# Run locally
./gradlew :frontend:apps-desktop:run

# Package native bundle (.dmg on macOS, .msi on Windows, .deb on Linux)
./gradlew :frontend:apps-desktop:packageDistributionForCurrentOS
```

### 2. Android
The Android application uses Jetpack Compose Material 3 and Firebase Authentication / Cloud Firestore.
```bash
# Compile debug APK
./gradlew :frontend:apps-android:assembleDebug

# Install directly to an active emulator or USB-connected device
./gradlew :frontend:apps-android:installDebug
```

### 3. Web (Wasm & JS)
The Web app compiles using Compose Multiplatform for Web:
```bash
# Development server with hot reload (Wasm)
./gradlew :frontend:apps-web:wasmJsBrowserDevelopmentRun

# Build production bundle
./gradlew :frontend:apps-web:wasmJsBrowserDistribution
```

### 4. iOS
The iOS app uses the shared Kotlin framework compiled for Apple Silicon / iOS Simulator targets:
- Open `frontend/apps/ios/iosApp.xcworkspace` in Xcode.
- Select your target simulator (e.g. iPhone 16 Pro) and press **Cmd+R** to build and run.
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
