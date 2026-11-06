---
sessionId: session-261105-235705-16cq
---

# Requirements

### Goal

Determine whether the three proposed Gradle version resolution methods (`strictly` in TOML, `resolutionStrategy.force()`, and `enforcedPlatform`) can prevent Gradle from downloading unused artifacts, and if not, clarify what they actually do and what options exist for the stated goal.

### Current State

The project already has a well-structured version catalog setup:
- **Three catalogs**: `ui/kmp/gradle/libs.versions.toml` (225 lines), `records/gradle/libs.versions.toml` (24 lines), `ui/kotlin-toolchain/libs.versions.toml` (10 lines)
- **Typed accessor layer**: `CatalogTypes.kt` provides strongly-typed enum wrappers over catalog lookups
- **Convention plugins**: Dependency convention plugins (`ComposeDependenciesConventionPlugin`, etc.) already consume catalog entries programmatically
- **Module build scripts**: Individual modules (e.g., `features/sign-in/data/build.gradle.kts`) use `libs.*` accessors
- **No raw GAV strings**: All dependencies use `libs.*` catalog accessors.

### What We Want

Reduce or eliminate Gradle downloading artifacts that are not actually used by the project.

### Out of Scope

- Declaration-style enforcement (preventing raw GAV strings in build scripts).
- Dependency verification (checksums/signatures).


# Technical Design

### Current Architecture

```
ui/kmp/
├── gradle/libs.versions.toml          # Main catalog (225 lines)
├── gradle/tools/project-plugins/      # Convention plugin project
│   ├── build.gradle.kts
│   └── src/main/kotlin/...
│       ├── CatalogTypes.kt            # Typed catalog accessor enums
│       └── plugins/config/            # Dependency convention plugins
├── settings.gradle.kts                # Includes gradle/tools as composite build
└── features/*/data/build.gradle.kts   # Module scripts using libs.*

records/
├── gradle/libs.versions.toml          # Records catalog (24 lines)
└── build.gradle.kts
```

### Answer: No, None of These Methods Prevent Downloading Unused Artifacts

The three proposed methods — `strictly` in TOML, `resolutionStrategy.force()`, and `enforcedPlatform` — all address **version conflict resolution**. They control which version wins when the same artifact appears at multiple versions in the dependency graph. They do nothing to prevent Gradle from downloading artifacts that are not used.

| Method | What it does | What it does NOT do |
|--------|--------------|---------------------|
| `strictly` in TOML | Forces a specific version when the artifact appears in the graph; fails if a conflicting version is requested | Prevent the artifact from being downloaded if it's not needed |
| `resolutionStrategy.force()` | Overrides version conflicts by forcing a version globally | Stop Gradle from resolving or downloading unused transitive dependencies |
| `enforcedPlatform` | Applies `strictly` constraints to all catalog entries as a BOM | Prevent downloading artifacts not declared in the platform |

**Gradle's documentation confirms this**: "Unused variables in the version catalog are ignored." A catalog entry that is never referenced in a `dependencies {}` block will never be resolved or downloaded. The three methods only matter when an artifact **is** in the graph and there's a version conflict.

### What Actually Causes Unused Downloads

Gradle downloads artifacts in two scenarios:

1. **Direct dependencies declared but unused in code**: If a module declares `implementation(libs.some.library)` but never references it in code, Gradle still downloads it because it's a declared dependency. The three methods don't address this.
2. **Transitive dependencies pulled in but unused**: If library A depends on library B, and your code never uses B's types, Gradle still downloads B because A requires it. The three methods don't address this either.

### What Actually Reduces Unused Downloads

| Approach | How it works | Applicability |
|----------|--------------|---------------|
| **Remove unused direct dependencies** | Run dependency analysis (e.g., Gradle Dependency Analysis plugin) to find declared dependencies never referenced in code, then remove them | High impact; catches dependencies no longer needed |
| **Exclude specific transitive dependencies** | Add `exclude(group, module)` on a per-declaration basis for transitive dependencies known to be unused | Targeted; requires knowing which transitive deps are unused |
| **Disable transitive dependencies per-configuration** | Set `configuration.isTransitive = false` on specific configurations | Broad; risks breaking dependencies that are actually needed |
| **Gradle Dependency Analysis plugin** | Automatically detects unused, unused-transitive, and redundant dependencies; provides advice to remove them | Best automated option; provides a report and actionable advice |
| **`./gradlew dependencies` inspection** | Manually review the dependency tree per module to identify unnecessary entries | Manual; good for periodic audits |

### Recommendation: Gradle Dependency Analysis Plugin (DAGP)

The **Gradle Dependency Analysis plugin** (DAGP, by autonomousapps) is the only tool that directly addresses the goal of reducing unused artifact downloads. Below is a detailed explanation of what it is, how it works, and how it applies to this project.

#### What is DAGP?

DAGP is a third-party Gradle plugin (latest version **3.19.2**) that performs static analysis on your project's dependency declarations and compiled code to produce actionable advice about your dependency graph. It supports JVM, Android, and Kotlin Multiplatform (KMP) projects. It is the most mature and widely-used tool for dependency hygiene in the Gradle ecosystem.

#### What DAGP Does

DAGP analyzes your project and reports five categories of dependency-related issues:

1. **Unused dependencies** — dependencies you declared (e.g., `implementation(libs.some.lib)`) but never reference in code. These are downloaded and resolved but serve no purpose. Removing them directly reduces downloads.
2. **Used transitive dependencies** — dependencies you use in code but only get transitively (not declared directly). DAGP advises you to declare them directly for stability and explicit control.
3. **Wrong configuration** — dependencies declared on the wrong scope (e.g., `api` vs `implementation` vs `compileOnly`). For example, a dependency only used at runtime should be `runtimeOnly`, not `implementation`.
4. **`compileOnly` candidates** — dependencies used at compile time but not required at runtime. Moving them to `compileOnly` prevents unnecessary runtime downloads.
5. **Unused annotation processors** — annotation processor declarations that are not processing any annotations in the module.

Additionally, DAGP reports **plugin-related issues**, such as:
- The Kotlin JVM plugin is applied but the module doesn't compile Kotlin code.
- The Android library plugin is applied but the module could be a plain JVM module.

#### How DAGP Works

DAGP uses **Kotlin metadata analysis** (via `kotlin-metadata-jvm`) to scan your project's compiled class files and determine which external types are actually referenced. It then compares those references against your declared dependency graph to produce advice. This is a form of static analysis — it reads the compiled output, not the source code directly.

#### Tasks Provided by DAGP

| Task | Description |
|------|-------------|
| `./gradlew :buildHealth` | Analyzes the entire project (all subprojects, all variants) and produces a report. Fails the build if violations are found. |
| `./gradlew :projectHealth` | Analyzes a single module (e.g., `./gradlew :features-sign-in-data:projectHealth`). |
| `./gradlew fixDependencies` | Auto-fixes all reported issues by modifying `build.gradle.kts` files. **Note: Does not work correctly for KMP projects yet.** |
| `./gradlew fixDependencies --upgrade` | Safe mode: only adds or upgrades declarations (e.g., `implementation` → `api`), never removes. |

#### Report Output

When you run `buildHealth`, DAGP:
1. **Prints advice to the console** in a readable format (if `printAdvice = true` is set).
2. **Writes a text report** to `build/reports/dependency-analysis/build-health-report.txt`.
3. **Writes a JSON report** for automated post-processing.
4. **Fails the build** if there are violations (configurable via `issues { all { severity = ... } }`).

Example console output:
```
> Task :buildHealth FAILED
FAILURE: Build failed with an exception.

What went wrong:
Execution failed for task ':buildHealth'.
> There were dependency violations. See report at
  file:///path/to/project/build/reports/dependency-analysis/build-health-report.txt
```

#### How to Apply DAGP to This Project

Two methods are available:

**Method A: Manual application (recommended for control)**

In `ui/kmp/build.gradle.kts` (root build):
```kotlin
plugins {
    id("com.autonomousapps.dependency-analysis") version "3.19.2"
    // Required: Kotlin and Android plugins must be on the same classloader
    id("org.jetbrains.kotlin.multiplatform") apply false
    id("com.android.application") apply false
}
```

In each subproject's `build.gradle.kts`:
```kotlin
plugins {
    id("com.autonomousapps.dependency-analysis")
}
```

**Method B: Build-health plugin (automatic, Gradle 8.8+)**

In `ui/kmp/settings.gradle.kts`:
```kotlin
plugins {
    id("com.autonomousapps.build-health") version "3.19.2"
}
```

This automatically applies DAGP to all subprojects. Requires Gradle 8.8+.

#### KMP-Specific Considerations

- DAGP has **experimental KMP support**. The analysis works, but the `fixDependencies` auto-remediation task does not work correctly for KMP projects yet.
- You will need to manually apply the advice (remove unused dependencies, change configurations) rather than relying on auto-fix.
- The plugin author published a dedicated blog post on KMP support and claims it can cut build time by 70% in KMP projects by eliminating unused dependencies.

#### How DAGP Reduces Unused Downloads

The mechanism is straightforward:
1. DAGP tells you which declared dependencies are unused.
2. You remove those declarations from your `build.gradle.kts` files.
3. Gradle no longer resolves or downloads those artifacts because they are no longer in the dependency graph.

This is the most effective approach because:
- **Gradle only downloads what is declared**. If nothing references a library in a `dependencies {}` block, it is never downloaded. Catalog entries that are never used are ignored.
- **Unused direct dependencies are the low-hanging fruit**. They are easy to remove and immediately reduce the dependency graph size.
- **Transitive exclusions are harder**. Removing a transitive dependency requires either excluding it (fragile) or replacing the parent library. DAGP helps you identify which transitive dependencies are used so you can make informed decisions.

#### Configuration Options

DAGP is highly configurable via the `dependencyAnalysis` DSL:

```kotlin
dependencyAnalysis {
    issues {
        all {
            // severity: "warn", "fail", or "ignore"
            severity = "warn"  // start with warnings, not build failures
        }
        // Ignore specific advice types
        usedTransitiveDependencies {
            severity = "ignore"  // don't warn about used transitives
        }
    }
    // Print advice to console
    printAdvice = true
}
```

#### Risks and Caveats

- **False positives for runtime-only usage**: Dependencies used via reflection, service loading (e.g., `META-INF/services`), or dynamic class loading may be reported as unused. Always verify with tests after removal.
- **`fixDependencies` is broken for KMP**: You must apply advice manually for this project.
- **Initial noise**: A first run on a large project will produce many findings. Start with `severity = "warn"` and address findings incrementally.
- **Configuration overhead**: DAGP adds to configuration time as it analyzes compiled metadata. Negligible for this project size.
- **Plugin classloader requirements**: Kotlin and Android plugins must be loaded in the root build script's classloader (with `apply false`) for DAGP to work correctly.

### What the Three Methods Are Useful For

Although they don't reduce unused downloads, the three methods are still valuable for **version governance**:

- **`strictly` in TOML**: Best for pinning critical dependencies (Firebase, Kotlin, AGP) where version drift causes real problems. Applied selectively to ~10–15 critical entries.
- **`resolutionStrategy.force()`**: Not recommended — it masks conflicts silently.
- **`enforcedPlatform`**: Not practical — requires publishing the catalog as a module.

These are complementary to reducing unused downloads, not substitutes.

### Risks

- **Removing unused dependencies blindly**: A dependency might be used only at runtime (reflection, service loading) and not detected by static analysis. Always verify with tests after removal.
- **Excluding transitive dependencies**: If a library updates and starts using an excluded transitive dependency, the build will break. Document exclusions and review periodically.
- **`strictly` is fragile in large graphs**: If a transitive dependency requires a version outside the `strictly` range, resolution fails. Start with a small set of critical dependencies.

