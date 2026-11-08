# Internalize Feature Data Sources & Enforce via Konsist Plan

## Goal Description
1. Update reference templates (`features/settings` and `features/job-applications-list`) and define Konsist test rules to mark all `*[Remote|Network|Local]DataSource*` definitions (both interfaces and `*Impl` classes) as **`internal`** to their respective Gradle modules.
2. Ensure only `*Repository` contracts (defined in domain) and `*RepositoryImpl` (defined in data) cross module boundaries as public/bound APIs.
3. Add a Konsist test rule enforcing that all data source interfaces and classes are `internal`.

## User Review Required
- **Internal Data Sources**: Shifting `*DataSource` interfaces and implementations from `public` to `internal` visibility within data modules, leaving repositories as the sole public data access API.
- **Konsist Enforcement**: Adding an automated rule to prevent any data source from being exposed as `public`.

## Proposed Changes

### 1. Konsist Test Rule
#### [MODIFY] [KonsistArchitectureTest.kt](file:///Users/fnc314/Work/Search/waleedtarbosh-ai-job-search/ui/kmp/apps/core/src/jvmTest/kotlin/dev/fnc314/tools/jobtracker/architecture/KonsistArchitectureTest.kt)
- Add test: `all datasource interfaces and implementations are internal`.

### 2. Refactor Template Data Modules (`features/settings/data`, `features/job-applications-list/data`)
- Change `public interface *DataSource` to `internal interface *DataSource` and ensure `internal class *Impl` is internal.

## Verification Plan

### Automated Tests
- Run Konsist tests: `./gradlew :apps-core:jvmTest --tests dev.fnc314.tools.jobtracker.architecture.KonsistArchitectureTest`
- Compile metadata: `./gradlew compileCommonMainKotlinMetadata`
- Assemble Android debug APK: `mise run ui:kmp:android:assemble`
