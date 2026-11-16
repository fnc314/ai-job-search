# Architecture Plan: Single Abstract Method (SAM) Fun Interface and Operator Invoke Standardization

**Status:** Completed  
**Date:** 2026-11-16  
**Target:** repository-wide  
**Harness:** Antigravity  
**Model:** gemini-3.8-flash  
**Session:** 54d0dcce-3356-4b69-96b7-39111a4682a5  

---

## 1. Executive Summary & Goals
This architectural plan establishes and enforces the repository-wide standard for all Kotlin Single Abstract Method (SAM) interfaces:
1. Every Kotlin `interface` containing exactly one abstract member to implement must be explicitly declared as a functional interface: `[public|internal] fun interface ...`.
2. The single abstract method must follow idiomatic Kotlin operator convention: `operator fun invoke(/* params */)`.
3. An automated Konsist test rule must be integrated into `tests/src/test/kotlin/` to permanently guard the repository against regressions.
4. AI authoring and code generation rules in `AGENTS.md` and `frontend/AGENTS.md` must be updated to mandate this pattern for all newly authored code.

---

## 2. Architecture & Design Principles
- **SAM Functional Interface Convention**: In Kotlin, `fun interface` allows callers to pass idiomatic lambda expressions directly without instantiating anonymous classes or boilerplate object expressions.
- **Invocable Operator Semantics**: Using `operator fun invoke(...)` enables instances of the interface to be called directly as functions (`instance(args)` instead of `instance.execute(args)` or `instance.register(args)`), reducing syntax overhead and unifying domain use cases, repositories with single queries, and event listeners.
- **Konsist Architectural Verification**: Consistency is verified and gated via automated static analysis in `tests` module using `Konsist`.

---

## 3. Inventory of Affected Interfaces & Refactoring Scope

### A. Core Data & Domain Layer
1. `frontend/core/data/.../repository/UseCase.kt`:
   - Updated `public interface UseCase<in Params, out Result>` to `public fun interface UseCase<in Params, out Result>`.
2. `frontend/features/sign-in/.../domain/usecase/GetCurrentUserUseCase.kt`:
   - Updated `public interface GetCurrentUserUseCase` to `public fun interface GetCurrentUserUseCase`.
3. `frontend/features/sign-in/.../domain/usecase/ObserveAuthStateUseCase.kt`:
   - Updated `public interface ObserveAuthStateUseCase` to `public fun interface ObserveAuthStateUseCase`.
4. `frontend/features/sign-in/.../domain/usecase/SignInUseCase.kt`:
   - Updated `public interface SignInUseCase` to `public fun interface SignInUseCase`.
5. `frontend/features/sign-in/.../domain/usecase/SignOutUseCase.kt`:
   - Updated `public interface SignOutUseCase` to `public fun interface SignOutUseCase`.
6. Parameterized Use Cases extending `UseCase<Params, Result>`:
   - `CreateJobApplicationUseCase.kt`, `CreateLeadUseCase.kt`, `LoadArtifactContentUseCase.kt`, `GetArtifactSignedUrlUseCase.kt`, `GetEventsUseCase.kt`, `LoadArtifactsUseCase.kt`, `LoadCompaniesUseCase.kt`, `LoadJobApplicationUseCase.kt`, `RecordEventUseCase.kt`, `FilterJobApplicationsUseCase.kt`, `LoadAllJobApplicationsUseCase.kt`, `SortJobApplicationsUseCase.kt`, `ApproveEditUseCase.kt`, `ExpireStaleEditsUseCase.kt`, `LoadPendingEditsUseCase.kt`, `RejectEditUseCase.kt`, `UpdateApplicationStatusUseCase.kt`.
   - Updated to `public fun interface ... : UseCase<Params, Result>`.

### B. Core Navigation
1. `frontend/core/navigation/.../FeatureEntryRegistry.kt`:
   - Updated to `public fun interface FeatureEntryRegistry { public operator fun invoke(scope: EntryProviderScope<PlatformNavKey>) }`.
   - Updated `App.kt` invocation from `entryRegistries.forEach { it.register(this) }` to `entryRegistries.forEach { it(this) }`.
   - Updated all 8 feature entry registry implementations (`*EntryRegistry.kt`) from `override fun register(...)` to `override operator fun invoke(...)`.

### C. Data Sources and Repositories
1. `frontend/features/companies-list/.../data/repository/CompaniesListRepository.kt`:
   - Updated to `public fun interface CompaniesListRepository { public suspend operator fun invoke(forceRefresh: Boolean): List<Company> }`.
2. `frontend/features/companies-list/.../data/source/RemoteCompaniesListDataSource.kt`:
   - Updated to `internal fun interface RemoteCompaniesListDataSource { public suspend operator fun invoke(): List<Company> }`.
3. `frontend/features/job-applications-list/.../data/repository/JobApplicationsListRepository.kt`:
   - Updated to `public fun interface JobApplicationsListRepository { public suspend operator fun invoke(forceRefresh: Boolean): List<JobApplication> }`.
4. `frontend/features/company-details/.../data/repository/CompanyDetailsRepository.kt`:
   - Updated from `getCompanyDetails(companyId: String)` to `public fun interface CompanyDetailsRepository { public suspend operator fun invoke(companyId: String): CompanyWithApplications? }`.
   - Updated `CompanyDetailsRepositoryImpl.kt`, `LoadCompanyDetailsUseCase.kt`, and test mocks to invoke operator.
5. `frontend/features/artifact-previewer/.../data/repository/ArtifactPreviewerRepository.kt`:
   - Updated from `fetchContent(url: String)` to `public fun interface ArtifactPreviewerRepository { public suspend operator fun invoke(url: String): String? }`.
   - Updated `ArtifactPreviewerRepositoryImpl.kt` and `LoadArtifactContentUseCaseImpl.kt` to invoke operator.
6. `frontend/features/job-application-details/.../data/source/NetworkJobApplicationDetailsDataSource.kt`:
   - Updated from `getArtifactSignedUrl(path: String)` to `internal fun interface NetworkJobApplicationDetailsDataSource { suspend operator fun invoke(path: String): String }`.
   - Updated `NetworkJobApplicationDetailsDataSourceImpl.kt` and `JobApplicationDetailsRepositoryImpl.kt`.

### D. Backend Subsystem
1. `backend/company-metadata-enrichment/.../HunterClient.kt`:
   - Updated `public interface HunterClient` to `public fun interface HunterClient`.

---

## 4. Verification Plan & Testing Checklist
- [x] Add Konsist architecture test rule enforcing `fun interface` and `operator fun invoke` for all SAM interfaces across `frontend`, `backend`, `records`, and `gradle/tools`.
- [x] Run test suite `./gradlew :tests:test --tests "dev.fnc314.tools.jobtracker.architecture.SingleAbstractMethodArchitectureTest"` to verify all Konsist architecture rules pass cleanly.
- [x] Update repository guidelines in root `AGENTS.md` and `frontend/AGENTS.md`.
- [x] Verify clean build across compilation targets: `:backend:company-metadata-enrichment:compileKotlin`, `:project-plugins:compileKotlin`, and `:frontend:apps-core:compileCommonMainKotlinMetadata`.

---

## 5. Bookkeeping
- [x] Plan persisted at `.agents/plans/2026-11-16/041546_sam-fun-interface-and-operator-invoke-standardization.md`
- [x] Registry `README.md` row added / status synchronized
- [ ] Craft a single commit of all changed files
- [ ] Push `git push origin main`
