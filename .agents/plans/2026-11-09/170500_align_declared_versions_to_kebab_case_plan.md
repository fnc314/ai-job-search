# Implementation Plan: Align Declared Versions into Kebab-Casing Format

**Status:** Draft  
**Date:** 2026-11-09  
**Target:** `gradle/libs.versions.toml` & version catalog consumers  
**Evaluates:** Systematic kebab-case normalization for all catalog `[versions]` keys, `version.ref` synchronization, `CatalogTypes` key updates, and Kotlin build script type-safe accessor alignment  
**Schema Authority:** [`tracker.schema.json`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/tracker.schema.json)  

---

## 1. Goal Description

Currently, the `[versions]` table in [`gradle/libs.versions.toml`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/gradle/libs.versions.toml) contains an inconsistent mix of camelCase, kebab-case, and duplicate aliases (e.g., `android-compileSdk`, `composeMultiplatform`, `googlePlayServices`, `javaToolchain` vs `java-toolchains`).

This change systematically normalizes **100% of declared version keys** in `[versions]` into strict **kebab-case** format, unifies duplicate Java toolchain aliases to `java-toolchain = "17"`, synchronizes all `version.ref` references across `[libraries]` and `[plugins]`, aligns `CatalogTypes.kt` lookup keys, updates Gradle typed accessors in consuming buildscripts, and formats both catalogs with `tombi format` ensuring 2-space table entry indentations.

---

## 2. User Review Required

> [!IMPORTANT]
> **Gradle Type-Safe Accessor Mapping**:
> When a catalog version key contains hyphens (`-`), Gradle's generated type-safe project accessors create nested properties for each hyphen:
> - `android-compileSdk` was accessed as `libs.versions.android.compileSdk`
> - `android-compile-sdk` will be accessed as `libs.versions.android.compile.sdk`
> - `java-toolchain` will be accessed as `libs.versions.java.toolchain`
> 
> Build files referencing `libs.versions.*` directly ([`apps/android/build.gradle.kts`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/apps/android/build.gradle.kts), [`records/parser/build.gradle.kts`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/records/parser/build.gradle.kts), [`records/proto/build.gradle.kts`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/records/proto/build.gradle.kts), and [`project-plugins/build.gradle.kts`](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/frontend/gradle/tools/project-plugins/build.gradle.kts)) will be updated to match the new nested accessor paths.

> [!NOTE]
> **Java Toolchain Value Unification**:
> Per earlier guidance, duplicate keys `javaToolchain = "26"` and `java-toolchains = "26"` are consolidated into a single kebab-case key:
> ```toml
> java-toolchain = "17"
> ```
> Setting this to `"17"` aligns the Kotlin compilation toolchains across all modules with `jvm = "17"`, and automatically resolves the Detekt `--jvm-target 26` error without ad-hoc workarounds.

---

## 3. Inventory of Keys to Align

| Current Key in `[versions]` | Target Kebab-Case Key | References in `[libraries]` / `[plugins]` | Build Scripts / `CatalogTypes` Affected |
| :--- | :--- | :--- | :--- |
| `android-compileSdk` | `android-compile-sdk` | N/A (consumed directly) | `apps/android/build.gradle.kts`, `CatalogTypes.kt` |
| `android-minSdk` | `android-min-sdk` | N/A (consumed directly) | `apps/android/build.gradle.kts`, `CatalogTypes.kt` |
| `android-targetSdk` | `android-target-sdk` | N/A (consumed directly) | `apps/android/build.gradle.kts`, `CatalogTypes.kt` |
| `androidx-testExt` | `androidx-test-ext` | `androidx-testExt-junit` | N/A |
| `androidx-testRunner` | `androidx-test-runner` | `androidx-testRunner` | N/A |
| `composeMultiplatform` | `compose-multiplatform` | `compose-*`, `compose-gradle-plugin`, `composeMultiplatform` plugin | N/A |
| `composeMaterialIcons` | `compose-material-icons` | (retained for icon dependencies) | N/A |
| `composeConstraintLayout` | `compose-constraint-layout`| `compose-constraint` | N/A |
| `javaToolchain` / `java-toolchains` | `java-toolchain` | N/A | `records/parser`, `records/proto`, `project-plugins`, `CatalogTypes.kt` |
| `googlePlayServices` | `google-play-services` | `google-play-services` plugin | N/A |
| `protobufGradlePlugin` | `protobuf-gradle-plugin` | `protobuf` plugin | N/A |
| `androidGradlePlugin` | `android-gradle-plugin` | `android-gradle-plugin` | N/A |

---

## 4. Proposed Changes

### Component 1: Version Catalogs

#### [MODIFY] `gradle/libs.versions.toml` & `gradle/tools/version-catalogs/src/main/resources/libs.versions.toml`
- Normalize all 12 camelCase version keys in `[versions]` to strict kebab-case.
- Consolidate `javaToolchain = "26"` and `java-toolchains = "26"` to `java-toolchain = "17"`.
- Update all corresponding `version.ref = "..."` attributes in `[libraries]` and `[plugins]`.
- Format with `tombi format`.

```toml
[versions]
  agp = "9.5.1"
  android-compile-sdk = "37"
  android-min-sdk = "24"
  android-target-sdk = "37"
  androidx-activity = "1.13.0"
  androidx-appcompat = "1.8.0"
  androidx-core = "1.19.1"
  androidx-espresso = "3.7.0"
  androidx-lifecycle = "2.11.0"
  androidx-test-ext = "1.3.0"
  androidx-test-runner = "1.7.0"
  androidx-credentials = "1.6.0"
  androidx-room3 = "3.0.3"
  androidx-sqlite = "2.7.1"
  google-id = "1.2.1"
  compose-multiplatform = "1.12.2"
  compose-material-icons = "1.7.3"
  compose-constraint-layout = "1.1.2"
  firebase-bom = "35.1.0"
  firebase-auth = "25.0.0"
  firebase-firestore = "27.1.0"
  firebase-admin = "9.5.0"
  junit = "4.13.2"
  koin = "4.2.2"
  koin-compiler = "1.2.3"
  kotlin = "2.4.21"
  kotlin-wrappers = "2026.11.0"
  kotlinx-collections-immutable = "0.5.2"
  kotlinx-coroutines = "1.11.0"
  kotlinx-datetime = "0.8.0"
  kotlinx-serialization = "1.12.0"
  ktor = "3.6.1"
  material3 = "1.12.0-alpha03"
  mockk = "1.14.12"
  navigation-compose = "2.10.0"
  navigation3 = "1.2.1"
  jvm = "17"
  java-toolchain = "17"
  google-play-services = "4.5.0"
  node = "26.13.0"
  ksp = "2.3.13"
  dokka = "2.3.0"
  detekt = "1.23.8"
  compose-rules = "0.6.8"
  konsist = "0.18.1"
  records = "1.0.0"
  kmp-inspector = "1.0.2"
  desugar = "2.1.5"
  coil = "3.6.3"
  protobuf-gradle-plugin = "0.9.4"
  protobuf = "4.28.2"
  android-gradle-plugin = "8.5.1"
  clikt = "5.0.3"
  mordant = "3.0.2"
```

---

### Component 2: Build Plugins & CatalogTypes

#### [MODIFY] `frontend/gradle/tools/project-plugins/src/main/kotlin/dev/fnc314/tools/jobtracker/kmp/gradle/catalog/CatalogTypes.kt`
Update catalog key strings to kebab-case:
```kotlin
    public enum class Versions(
        override val catalogKey: String,
    ) : CatalogTypes<VersionConstraint> {
        JVM("jvm"),
        JAVA_TOOLCHAIN("java-toolchain"),
        ANDROID_COMPILE_SDK("android-compile-sdk"),
        ANDROID_MIN_SDK("android-min-sdk"),
        ANDROID_TARGET_SDK("android-target-sdk"),
        KOTLIN("kotlin"),
        AGP("agp"),
        NODE("node"),
        DETEKT("detekt"),
        ;
```

#### [MODIFY] `frontend/gradle/tools/project-plugins/build.gradle.kts`
Update accessor from `libs.versions.javaToolchain` to:
```kotlin
kotlin {
  explicitApi()
  jvmToolchain(
    libs.versions.java.toolchain.get().toInt()
  )
}
```

---

### Component 3: Subsystem Build Scripts

#### [MODIFY] `frontend/apps/android/build.gradle.kts`
Update SDK accessors to reflect nested hyphen structure:
```kotlin
android {
  namespace = "dev.fnc314.tools.jobtracker.app.android"
  compileSdk = libs.versions.android.compile.sdk.get().toInt()

  defaultConfig {
    applicationId = "dev.fnc314.tools.jobtracker"
    minSdk = libs.versions.android.min.sdk.get().toInt()
    targetSdk = libs.versions.android.target.sdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }
```

#### [MODIFY] `records/parser/build.gradle.kts`
Update toolchain accessor:
```kotlin
kotlin {
  jvmToolchain(libs.versions.java.toolchain.get().toInt())
  explicitApiWarning()
```

#### [MODIFY] `records/proto/build.gradle.kts`
Update toolchain accessor:
```kotlin
kotlin {
  jvmToolchain(libs.versions.java.toolchain.get().toInt())
```

---

## 5. Verification Plan

### Automated Verification
```bash
# 1. Verify TOML formatting with tombi
tombi format --check gradle/libs.versions.toml
tombi format --check gradle/tools/version-catalogs/src/main/resources/libs.versions.toml

# 2. Build root composite build to regenerate version catalog accessors & verify compilation
./gradlew buildAll

# 3. Verify records build & tests with the new kebab-cased java-toolchain (17)
./gradlew :records:test

# 4. Verify Detekt runs cleanly across records and frontend
./gradlew :records:detekt :frontend:detekt

# 5. Verify Android app compiles with the new android.compile.sdk accessors
./gradlew :frontend:apps-android:assembleDebug
```

### Manual Verification
- Inspect generated `buildAll` logs to confirm zero catalog lookup warnings and no missing version errors.

---

## 6. Bookkeeping & Completion
- Plan registered at `.agents/plans/2026-11-09/170500_align_declared_versions_to_kebab_case_plan.md`
- Registry `README.md` updated
- Commit message: `170500_align_declared_versions_to_kebab_case_plan.md`
- Push to `origin main`
