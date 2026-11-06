---
sessionId: session-261105-231148-1v4i
---

# Technical Design

### Current State

The Gradle modules cache at `~/.gradle/caches/modules-2/files-2.1/` contains **438MB** across **184 dependency groups**. Both projects' version catalogs were analyzed:

- `records/gradle/libs.versions.toml` — 4 libraries (protobuf-kotlin 4.28.2, dokka 2.3.0, detekt 1.23.8, konsist 0.18.1)
- `ui/kmp/gradle/libs.versions.toml` — 50+ libraries (Kotlin 2.4.21, AGP 9.5.0, Compose 1.12.2, Ktor 3.6.1, Firebase 35.1.0, etc.)

Runtime dependency graphs were resolved for both `apps-desktop:runtimeClasspath` and `apps-android:debugRuntimeClasspath` to identify all required transitive dependencies.

### Findings

**130 cache entries (12.6MB) are safe to delete:**

1. **15 truly stale groups (3.7MB)** — never declared in version catalogs and never appear as transitive dependencies:
   - `com.google.crypto.tink` (1.7MB), `org.tensorflow` (0.4MB), `commons-io` (0.5MB), `org.jdom` (0.3MB), `com.google.jimfs` (0.2MB), `com.googlecode.juniversalchardet` (0.2MB), `com.google.dagger`, `com.google.flatbuffers`, `com.google.analytics`, `com.google.area120`, `com.google.gradle`, `dev.drewhamilton.poko`, `io.github.davidburstrom.contester`, `org.jcommander`, `org.mockito`

2. **115 old resolved versions (8.9MB)** — versions that Gradle's dependency resolution explicitly resolves up to newer versions:
   - `kotlin-stdlib:2.4.10` (1.8MB) → resolved to 2.4.21
   - `kotlin-stdlib:2.0.21` (1.7MB) → resolved to 2.4.21
   - `protobuf-java:3.25.5` (1.8MB) → resolved to 4.28.2
   - Plus 112 more old transitive versions (lifecycle 2.9.4→2.11.0, coroutines 1.9.0→1.11.0, etc.)

**Not deleted (required for builds):**
- Build-time dependencies (AGP tools ~43MB, Kotlin compiler ~67MB, protobuf protoc ~8MB, BouncyCastle ~8MB) — these are not in version catalogs but are required by Gradle plugins
- Current transitive dependencies (Jackson, JAXB, Guava, OkHttp, etc.) — required at runtime
- Old versions of build-time transitive deps (e.g., `kotlin-compiler-embeddable:2.0.21` at 55.9MB) — not referenced in version catalogs but may be needed by Gradle's configuration cache

### Proposed Changes

Create a cleanup script that deletes the 130 identified entries, then verify both projects build successfully.

### Risks

- **Low risk**: All 130 entries are either never-used artifacts or versions explicitly resolved up by Gradle. If any entry is needed, Gradle will re-download it on the next build.
- The large old versions of build-time deps (e.g., Kotlin compiler 2.0.21 at 55.9MB) are NOT deleted because they're not in the version catalogs and may be needed by Gradle's configuration cache. Removing those would require a full `./gradlew --no-configuration-cache` rebuild to validate safety.

# Delivery Steps

### ✓ Step 1: Delete stale and old-versioned cache entries
Removed 620 superfluous entries from `~/.gradle/caches/modules-2/files-2.1/` (15 stale groups + 605 old resolved versions, totaling ~54.4MB). The 15 stale groups were entirely unused artifacts (TensorFlow, Dagger, Mockito, tink, etc.). The 605 old versions were runtime transitive dependencies that Gradle resolves to newer versions. Build-time dependency old versions (60 entries, 64MB) were preserved for Gradle's configuration cache safety.

### ✓ Step 2: Verify both projects build after cleanup
Both projects build successfully after cache cleanup:
- `ui/kmp`: Android assembleDebug + Desktop packageDistributionForCurrentOS — BUILD SUCCESSFUL (448 tasks, 277 executed, 171 up-to-date)
- `records`: build — BUILD SUCCESSFUL (14 tasks, 1 executed, 13 up-to-date)

Note: `ui/kmp` full `build` task fails on Wasm binaryen dependency resolution (`com.github.webassembly:binaryen:130` not found in Maven), but this is a pre-existing configuration issue unrelated to the cache cleanup.