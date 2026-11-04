---
sessionId: session-261104-004147-c2e2
---

# Requirements

### Overview
Add a `jobApplicationUrl` field to track the ATS application URL (where the application lives, e.g., Greenhouse/Workday submission page), distinct from the existing `jobUrl` which tracks the job description URL. This field flows through the entire stack: proto definition → KMP data model → form UI → detail view → Firestore persistence.

Additionally, make the `records` project (at `../../records`) consumable by the KMP app via standard Maven GAV notation by adding a Gradle publish plugin.

### Functional Requirements
1. **New field `jobApplicationUrl`** on `JobApplication`:
   - Optional string field (nullable)
   - Stored in the proto as field number 21 (next available after `metadata` at 20)
   - Shown in the "Add Application" form as a text input labeled "Application URL"
   - Displayed in the detail view alongside the existing `jobUrl`
   - Passed through `Factory.createJobApplication()` and the `AddJobApplicationViewModel`

2. **Records project publishable**:
   - `records-proto` module publishes a JAR with generated Kotlin code
   - `records-parser` module publishes a JAR depending on `records-proto`
   - KMP project can consume via `implementation("dev.fnc314.tools.jobsearch:records-proto:<version>")` using GAV notation

### Scope
- **In Scope**: Proto definition, KMP data model, form UI, detail UI, ViewModel, Factory, Firestore repository stub, records publish plugin
- **Out of Scope**: Actual Firestore implementation (stubs exist), iOS/desktop-specific code changes beyond commonMain, parser logic changes

# Technical Design

### Current Implementation
- **Proto**: `records/proto/src/main/proto/tracker/v1/application.proto` defines `JobApplication` with fields 1–20. Field 21 is the next available number.
- **KMP Model**: `shared/.../data/model/JobApplication.kt` is a data class with `jobUrl: String?` but no `jobApplicationUrl`.
- **Form**: `AddJobApplicationScreen.kt` has a `jobUrl` text field but no application URL field.
- **Detail**: `JobApplicationDetailScreen.kt` renders `jobUrl` in `ApplicationSummarySection`.
- **ViewModel**: `AddJobApplicationViewModel.kt` has `jobUrl` in `AddApplicationFormState` and passes it to `Factory.createJobApplication()`.
- **Factory**: `Factory.kt` accepts `jobUrl` parameter.
- **Records Project**: `records/proto/build.gradle.kts` compiles protobufs but has no `maven-publish` plugin. Same for `records/parser/build.gradle.kts`.

### Key Decisions
- **Field name**: `jobApplicationUrl` (the ATS application URL) vs `jobUrl` (the job description URL)
- **Proto field number**: 21 (next after `metadata` at 20)
- **Publish approach**: Add `maven-publish` plugin to both `records-proto` and `records-parser` with GAV coordinates `dev.fnc314.tools.jobsearch:records-proto` and `dev.fnc314.tools.jobsearch:records-parser`. Use `localPublish` task pattern so the KMP project can consume via `mavenLocal()` or a flat dir repo.
- **KMP consumption**: Add `records-proto` as a `jvmMain` dependency (since proto-generated Kotlin is JVM-only) to enable the KMP app to use the typed proto models on JVM targets.

### Proposed Changes

#### Records Project (publish plugin)
1. **`records/proto/build.gradle.kts`**: Add `maven-publish` plugin with GAV `dev.fnc314.tools.jobsearch:records-proto:1.0.0`. Configure publication to include the generated Kotlin classes from protobuf.
2. **`records/parser/build.gradle.kts`**: Add `maven-publish` plugin with GAV `dev.fnc314.tools.jobsearch:records-parser:1.0.0`.
3. **`records/build.gradle.kts`**: Add `group` and `version` at the root level so subprojects inherit.

#### Proto Definition
4. **`application.proto`**: Add `string jobApplicationUrl = 21;` to the `JobApplication` message.

#### KMP Data Model
5. **`JobApplication.kt`**: Add `val jobApplicationUrl: String?` field.
6. **`Factory.kt`**: Add `jobApplicationUrl: String? = null` parameter.

#### KMP Form UI
7. **`AddApplicationFormState`**: Add `jobApplicationUrl: String = ""` property.
8. **`AddJobApplicationViewModel`**: Add `updateJobApplicationUrl()` method and pass to `Factory.createJobApplication()`.
9. **`AddJobApplicationScreen.kt`**: Add `OutlinedTextField` for "Application URL" between Job URL and Job ID fields.

#### KMP Detail UI
10. **`JobApplicationDetailScreen.kt`**: Render `jobApplicationUrl` in `ApplicationSummarySection` below `jobUrl`.

#### KMP Dependency on Records
11. **`shared/build.gradle.kts`**: Add `records-proto` as a `jvmMain` dependency via GAV notation.
12. **`gradle/libs.versions.toml`**: Add version catalog entry for records-proto.

### File Structure
```
records/
  build.gradle.kts                  # MODIFIED: add group/version
  proto/build.gradle.kts            # MODIFIED: add maven-publish
  parser/build.gradle.kts           # MODIFIED: add maven-publish
  proto/src/main/proto/tracker/v1/application.proto  # MODIFIED: add field 21

ui/kmp/
  gradle/libs.versions.toml         # MODIFIED: add records-proto version
  shared/build.gradle.kts           # MODIFIED: add records-proto dependency
  shared/src/commonMain/kotlin/.../
    data/model/JobApplication.kt    # MODIFIED: add jobApplicationUrl
    di/Factory.kt                   # MODIFIED: add jobApplicationUrl param
    ui/add/AddJobApplicationViewModel.kt  # MODIFIED: add form state + updater
    ui/add/AddJobApplicationScreen.kt     # MODIFIED: add URL text field
    ui/detail/JobApplicationDetailScreen.kt  # MODIFIED: render URL
```

# Delivery Steps

### ✓ Step 1: Preparations
Prerequisites for changes.
* The packaging for `records` would be `dev.fnc314.tools.jobsearch.records:records-*:1.0.0`
* Make `mavenLocal` not `~/.m2` but `./.m2` (project root) and mark in `.gitignore`
* Prepare a separate set of steps for enhancing the TUI with editing abilities for urls

### ✓ Step 2: Add maven-publish to records project
Make records-proto and records-parser publishable via standard GAV notation.
- Add `group` and `version` to `records/build.gradle.kts` root build file
- Add `maven-publish` plugin to `records/proto/build.gradle.kts` with publication config for GAV `dev.fnc314.tools.jobsearch:records-proto:1.0.0`
- Add `maven-publish` plugin to `records/parser/build.gradle.kts` with publication config for GAV `dev.fnc314.tools.jobsearch:records-parser:1.0.0`
- Run `./gradlew publishToMavenLocal` in records project to verify publication works

### ✓ Step 3: Add jobApplicationUrl to proto and KMP data model
Add the new field to the protobuf definition and propagate it through the KMP data layer.
- Add `string jobApplicationUrl = 21;` to `records/proto/src/main/proto/tracker/v1/application.proto`
- Add `val jobApplicationUrl: String?` to `JobApplication.kt` data class
- Add `jobApplicationUrl: String? = null` parameter to `Factory.createJobApplication()`

### ✓ Step 4: Add jobApplicationUrl to form UI and ViewModel
Wire the new field into the Add Application form so users can input it.
- Add `jobApplicationUrl: String = ""` to `AddApplicationFormState`
- Add `updateJobApplicationUrl()` method to `AddJobApplicationViewModel`
- Pass `jobApplicationUrl` to `Factory.createJobApplication()` in the `submit()` method
- Add `OutlinedTextField` labeled "Application URL" to `AddJobApplicationScreen.kt` between Job URL and Job ID fields

### ✓ Step 5: Add jobApplicationUrl to detail view and records dependency
Display the new field in the detail screen and wire up the records dependency in the KMP project.
- Render `jobApplicationUrl` in `ApplicationSummarySection` of `JobApplicationDetailScreen.kt` below the existing `jobUrl`
- Add records-proto version catalog entry to `gradle/libs.versions.toml`
- Add `records-proto` as a `jvmMain` dependency in `shared/build.gradle.kts` via GAV notation
- Verify the KMP project compiles with the new dependency